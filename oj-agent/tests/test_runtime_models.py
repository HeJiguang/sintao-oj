from app.runtime.enums import RiskLevel, RunStatus, TaskType
from app.runtime.models import (
    EvidenceItem,
    EvidenceState,
    ExecutionState,
    GuardrailState,
    OutcomeState,
    RequestContext,
    UnifiedAgentState,
    WriteIntent,
)
from app.runtime.checkpoints import CheckpointPayload


def test_unified_agent_state_preserves_partitioned_runtime_state():
    request = RequestContext(
        trace_id="trace-001",
        user_id="u-1",
        conversation_id="conv-1",
        task_type=TaskType.CHAT,
        user_message="Why is this WA?",
        question_id="1001",
        question_title="Two Sum",
    )
    execution = ExecutionState(
        run_id="run-001",
        graph_name="supervisor",
        status=RunStatus.RUNNING,
        active_node="router",
    )
    evidence = EvidenceState(
        items=[
            EvidenceItem(
                evidence_id="ev-1",
                source_type="knowledge_doc",
                source_id="doc-1",
                title="Two Sum Pattern",
                snippet="Use a hash map to track complements.",
                recall_score=0.91,
                rerank_score=0.88,
            )
        ]
    )
    guardrail = GuardrailState(
        risk_level=RiskLevel.LOW,
        completeness_ok=True,
        policy_ok=True,
    )
    outcome = OutcomeState(
        answer="Check whether your duplicate handling misses the second index.",
        next_action="Trace the failing sample.",
        write_intents=[
            WriteIntent(
                intent_type="training_plan_write",
                target_service="oj-friend",
                payload={"planTitle": "Starter Repair Plan"},
            )
        ],
    )

    state = UnifiedAgentState(
        request=request,
        execution=execution,
        evidence=evidence,
        guardrail=guardrail,
        outcome=outcome,
    )

    assert state.request.task_type is TaskType.CHAT
    assert state.execution.status is RunStatus.RUNNING
    assert state.evidence.items[0].source_id == "doc-1"
    assert state.guardrail.risk_level is RiskLevel.LOW
    assert state.outcome.write_intents[0].target_service == "oj-friend"


def test_checkpoint_payload_keeps_resume_pointer_and_state_snapshot():
    state = UnifiedAgentState(
        request=RequestContext(
            trace_id="trace-002",
            user_id="u-2",
            task_type=TaskType.TRAINING_PLAN,
            user_message="Generate my next training plan.",
        ),
        execution=ExecutionState(
            run_id="run-002",
            graph_name="plan_graph",
            status=RunStatus.RUNNING,
            active_node="verification",
        ),
        evidence=EvidenceState(),
        guardrail=GuardrailState(risk_level=RiskLevel.MEDIUM),
        outcome=OutcomeState(),
    )

    checkpoint = CheckpointPayload(
        checkpoint_id="cp-001",
        run_id="run-002",
        graph_name="plan_graph",
        node_name="verification",
        sequence_no=3,
        state=state,
    )

    assert checkpoint.node_name == "verification"
    assert checkpoint.state.execution.run_id == "run-002"
    assert checkpoint.state.request.task_type is TaskType.TRAINING_PLAN
