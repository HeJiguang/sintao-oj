from app.runtime.engine import execute_training_plan_request
from app.runtime.enums import RunStatus
from app.schemas.training_plan_request import QuestionCandidate, TrainingPlanRequest


def test_execute_training_plan_request_returns_unified_state_with_plan_payload():
    state = execute_training_plan_request(
        TrainingPlanRequest(
            trace_id="trace-training-runtime-001",
            user_id=1,
            current_level="starter",
            target_direction="algorithm_foundation",
            candidate_questions=[
                QuestionCandidate(
                    question_id=101,
                    title="Two Sum",
                    difficulty=1,
                    algorithm_tag="array",
                    knowledge_tags="hash table",
                    estimated_minutes=12,
                )
            ],
        )
    )

    assert state.execution.graph_name == "supervisor_graph"
    assert state.execution.status is RunStatus.SUCCEEDED
    assert state.outcome.intent == "training_plan"
    assert state.outcome.response_payload["plan_title"]
    assert state.outcome.response_payload["tasks"]
    assert state.outcome.write_intents
    assert state.outcome.write_intents[0].intent_type == "training_plan_write"
    assert state.outcome.write_intents[0].target_service == "oj-friend"
