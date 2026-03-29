from collections.abc import Iterator, Mapping

from app.guardrails.runtime import GuardrailRuntime
from app.runtime.engine import prepare_chat_stream_state, record_chat_state
from app.runtime.enums import RiskLevel
from app.runtime.models import UnifiedAgentState
from app.schemas.chat_request import ChatRequest
from app.schemas.stream_events import DeltaEvent, ErrorEvent, FinalEvent, MetaEvent, StatusEvent
from app.services import chat_assistant
from app.services.stream_emitter import to_sse_event


def _higher_risk(left: RiskLevel, right: RiskLevel) -> RiskLevel:
    order = {
        RiskLevel.LOW: 1,
        RiskLevel.MEDIUM: 2,
        RiskLevel.HIGH: 3,
    }
    return left if order[left] >= order[right] else right


def _assistant_state(state: UnifiedAgentState) -> dict:
    return dict(state.outcome.response_payload.get("assistant_state") or {})


def _finalize_state(
    state: UnifiedAgentState,
    *,
    answer: str,
    confidence: float,
    next_action: str,
    model_name: str | None,
) -> UnifiedAgentState:
    output_guard = GuardrailRuntime().evaluate_output(
        answer=answer,
        evidence_count=len(state.evidence.items),
    )
    guardrail = state.guardrail.model_copy(
        update={
            "risk_level": _higher_risk(state.guardrail.risk_level, output_guard.risk_level),
            "policy_ok": state.guardrail.policy_ok and output_guard.policy_ok,
            "risk_reasons": state.guardrail.risk_reasons + output_guard.risk_reasons,
        }
    )
    execution = state.execution.model_copy(
        update={
            "model_name": model_name or state.execution.model_name,
        }
    )
    outcome = state.outcome.model_copy(
        update={
            "answer": answer,
            "confidence": confidence,
            "next_action": next_action,
        }
    )
    return state.model_copy(
        update={
            "execution": execution,
            "guardrail": guardrail,
            "outcome": outcome,
        }
    )


def stream_chat_request(
    request: ChatRequest,
    headers: Mapping[str, str | None],
) -> Iterator[str]:
    state = prepare_chat_stream_state(request, headers)
    assistant_state = _assistant_state(state)

    yield to_sse_event(
        "meta",
        MetaEvent(
            trace_id=state.request.trace_id,
            graph_version="phase-1-runtime",
            mode="llm",
        ),
    )

    for status in state.outcome.status_events:
        yield to_sse_event(
            "status",
            StatusEvent(
                node=str(status.get("node") or "unknown"),
                message=str(status.get("message") or ""),
            ),
        )

    answer = ""
    model_name = state.execution.model_name
    streaming_error: str | None = None
    try:
        for chunk in chat_assistant.stream_chat_answer(assistant_state):
            answer += chunk
            yield to_sse_event("delta", DeltaEvent(answer=answer))
    except Exception as exc:  # pragma: no cover - defensive runtime fallback
        streaming_error = str(exc)

    if answer:
        confidence = float(assistant_state.get("confidence") or state.outcome.confidence or 0.2)
        next_action = str(
            assistant_state.get("next_action")
            or state.outcome.next_action
            or "Send one more concrete failing example."
        )
        chat_assistant.remember_generated_answer(assistant_state, answer, confidence, next_action)
        model_name = model_name or "streaming-runtime"
    else:
        answer, confidence, next_action, model_name = chat_assistant.generate_chat_answer(assistant_state)
        if streaming_error:
            yield to_sse_event("error", ErrorEvent(message=streaming_error))

    final_state = _finalize_state(
        state,
        answer=answer,
        confidence=confidence,
        next_action=next_action,
        model_name=model_name,
    )
    record_chat_state(final_state)

    yield to_sse_event(
        "final",
        FinalEvent(
            answer=answer,
            confidence=confidence,
            next_action=next_action,
        ),
    )
