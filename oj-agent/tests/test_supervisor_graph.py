from app.graphs.supervisor_graph import build_supervisor_graph
from app.runtime.context import build_request_context
from app.runtime.enums import RiskLevel, RunStatus, TaskType
from app.runtime.models import (
    EvidenceState,
    ExecutionState,
    GuardrailState,
    OutcomeState,
    UnifiedAgentState,
)


def test_supervisor_graph_routes_chat_requests_to_tutor_graph(monkeypatch):
    import app.graphs.supervisor_graph as supervisor_module  # noqa: WPS433

    class _StubTutorGraph:
        def invoke(self, payload):
            request = payload["request"]
            execution = payload["execution"].model_copy(
                update={
                    "status": RunStatus.SUCCEEDED,
                    "active_node": "tutor_graph",
                }
            )
            return {
                "unified_state": UnifiedAgentState(
                    request=request,
                    execution=execution,
                    evidence=EvidenceState(route_names=["stub"]),
                    guardrail=GuardrailState(
                        risk_level=RiskLevel.LOW,
                        completeness_ok=True,
                        policy_ok=True,
                    ),
                    outcome=OutcomeState(
                        intent="analyze_failure",
                        answer="Tutor graph answer",
                        confidence=0.9,
                        next_action="Trace the failing path.",
                    ),
                )
            }

    monkeypatch.setattr(supervisor_module, "build_tutor_graph", lambda: _StubTutorGraph())

    result = build_supervisor_graph().invoke(
        {
            "request": build_request_context(
                trace_id="trace-supervisor-001",
                user_id="u-1",
                task_type=TaskType.CHAT,
                user_message="Why is this WA?",
            )
        }
    )

    assert result["unified_state"].outcome.answer == "Tutor graph answer"
    assert result["unified_state"].execution.active_node == "tutor_graph"
