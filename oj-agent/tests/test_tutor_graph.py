from app.graphs.capabilities.tutor_graph import build_tutor_graph
from app.runtime.context import build_request_context
from app.runtime.enums import RiskLevel, RunStatus, TaskType
from app.runtime.models import ExecutionState


def test_tutor_graph_populates_evidence_and_guardrail_state(monkeypatch):
    import app.graphs.capabilities.tutor_graph as tutor_module  # noqa: WPS433
    from app.retrieval.models import RetrievalResult, RetrievedEvidence  # noqa: WPS433

    class _StubRetrievalRuntime:
        def retrieve(self, query):
            return RetrievalResult(
                route_names=["lexical"],
                items=[
                    RetrievedEvidence(
                        evidence_id="lex-1",
                        route_name="lexical",
                        source_type="knowledge_doc",
                        source_id="doc-1",
                        title="Hash Map Patterns",
                        snippet="Use a hash map to track complements.",
                        score=0.93,
                    )
                ],
            )

    class _StubGuardrailRuntime:
        def evaluate(self, guardrail_input):
            from app.guardrails.runtime import GuardrailOutput  # noqa: WPS433

            return GuardrailOutput(
                risk_level=RiskLevel.LOW,
                completeness_ok=True,
                policy_ok=True,
                missing_fields=[],
                risk_reasons=[],
            )

        def evaluate_output(self, *, answer, evidence_count):
            from app.guardrails.runtime import GuardrailOutput  # noqa: WPS433

            return GuardrailOutput(
                risk_level=RiskLevel.LOW,
                completeness_ok=True,
                policy_ok=True,
                missing_fields=[],
                risk_reasons=[],
            )

    class _StubLegacyGraph:
        def invoke(self, payload):
            return {
                "intent": "analyze_failure",
                "status_events": [
                    {"node": "router", "message": "Routed request to intent: analyze_failure."}
                ],
                "knowledge_hits": [],
                "context_gaps": [],
                "confidence": 0.88,
                "next_action": "Trace the smallest failing sample.",
                "final_answer": "Legacy answer",
            }

    monkeypatch.setattr(tutor_module, "RetrievalRuntime", lambda: _StubRetrievalRuntime())
    monkeypatch.setattr(tutor_module, "GuardrailRuntime", lambda: _StubGuardrailRuntime())
    monkeypatch.setattr(tutor_module, "build_graph", lambda: _StubLegacyGraph())
    monkeypatch.setattr(
        tutor_module.chat_assistant,
        "generate_chat_answer",
        lambda state: (
            "TutorGraph answer",
            0.91,
            "Trace the smallest failing sample.",
            "mock-model",
        ),
    )

    result = build_tutor_graph().invoke(
        {
            "request": build_request_context(
                trace_id="trace-tutor-001",
                user_id="u-1",
                task_type=TaskType.CHAT,
                user_message="Why is this WA?",
                question_title="Two Sum",
                judge_result="WA on sample #2",
                user_code="public class Solution {}",
            ),
            "execution": ExecutionState(
                run_id="run-tutor-001",
                graph_name="supervisor_graph",
                status=RunStatus.RUNNING,
                active_node="chat_capability",
            ),
        }
    )

    state = result["unified_state"]

    assert state.evidence.route_names == ["lexical"]
    assert state.evidence.items[0].source_id == "doc-1"
    assert state.guardrail.completeness_ok is True
    assert state.guardrail.policy_ok is True
    assert state.outcome.answer == "TutorGraph answer"
