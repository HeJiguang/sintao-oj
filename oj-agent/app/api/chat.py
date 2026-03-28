from collections.abc import Iterator

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse

from app.graph.edges import ANALYZE_FAILURE, ASK_FOR_CONTEXT, EXPLAIN_PROBLEM, RECOMMEND_QUESTION
from app.graph.builder import build_graph
from app.core.config import load_settings
from app.core.identity import normalize_chat_request
from app.schemas.chat_request import ChatRequest
from app.schemas.chat_response import ChatResponse
from app.schemas.stream_events import DeltaEvent, FinalEvent, MetaEvent, StatusEvent
from app.services import chat_assistant
from app.services.stream_emitter import to_sse_event


router = APIRouter(prefix="/api/chat", tags=["chat"])


def build_status_message(intent: str) -> str:
    status_by_intent = {
        ANALYZE_FAILURE: "Detected intent: analyze_failure. Preparing failure-analysis guidance.",
        EXPLAIN_PROBLEM: "Detected intent: explain_problem. Preparing problem explanation.",
        RECOMMEND_QUESTION: "Detected intent: recommend_question. Preparing recommendation guidance.",
        ASK_FOR_CONTEXT: "Detected intent: ask_for_context. Requesting more context.",
    }
    return status_by_intent.get(intent, f"Detected intent: {intent}")


def _build_state(request: ChatRequest) -> dict:
    graph = build_graph()
    return graph.invoke(request.model_dump())


@router.post("", response_model=ChatResponse)
def chat(request: ChatRequest, raw_request: Request) -> ChatResponse:
    normalized_request = normalize_chat_request(
        request,
        raw_request.headers,
        load_settings().gateway_user_id_header,
    )
    result = _build_state(normalized_request)
    answer, confidence, next_action, _model_name = chat_assistant.generate_chat_answer(result)
    return ChatResponse(
        trace_id=normalized_request.trace_id or result["trace_id"],
        intent=result["intent"],
        answer=answer,
        confidence=confidence,
        next_action=next_action,
    )


@router.post("/detail", response_model=ChatResponse)
def chat_detail(request: ChatRequest, raw_request: Request) -> ChatResponse:
    return chat(request, raw_request)


@router.post("/stream")
def chat_stream(request: ChatRequest, raw_request: Request) -> StreamingResponse:
    def event_stream() -> Iterator[str]:
        normalized_request = normalize_chat_request(
            request,
            raw_request.headers,
            load_settings().gateway_user_id_header,
        )
        yield to_sse_event(
            "meta",
            MetaEvent(trace_id=normalized_request.trace_id or "unknown-trace", graph_version="phase-2", mode="llm"),
        )

        result = _build_state(normalized_request)

        for status in result.get("status_events", []):
            yield to_sse_event(
                "status",
                StatusEvent(node=status["node"], message=status["message"]),
            )

        yield to_sse_event(
            "status",
            StatusEvent(node="intent", message=build_status_message(result["intent"])),
        )

        answer = ""
        try:
            for chunk in chat_assistant.stream_chat_answer(result):
                answer += chunk
                yield to_sse_event("delta", DeltaEvent(answer=answer))
        except Exception:
            pass

        if not answer:
            fallback_answer, confidence, next_action, model_name = chat_assistant.generate_chat_answer(result)
            answer = fallback_answer
        else:
            confidence = float(result.get("confidence") or 0.2)
            next_action = str(result.get("next_action") or "Send one more concrete failing example.")
            model_name = "streamed-llm"
            chat_assistant.remember_generated_answer(result, answer, confidence, next_action)

        yield to_sse_event(
            "final",
            FinalEvent(
                answer=answer,
                confidence=confidence,
                next_action=next_action,
            ),
        )

    return StreamingResponse(event_stream(), media_type="text/event-stream")
