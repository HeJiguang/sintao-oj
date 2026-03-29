from datetime import datetime, timedelta

from app.graphs.capabilities.plan_graph import build_plan_graph
from app.schemas.training_plan_request import QuestionCandidate, TrainingPlanRequest


def test_plan_graph_returns_training_plan_response():
    now = datetime.now()
    request = TrainingPlanRequest(
        trace_id="plan-graph-001",
        user_id=1,
        current_level="starter",
        target_direction="algorithm_foundation",
        preferred_count=3,
        candidate_questions=[
            QuestionCandidate(
                question_id=101,
                title="Two Sum",
                difficulty=1,
                algorithm_tag="array",
                knowledge_tags="hash table",
                estimated_minutes=12,
            ),
            QuestionCandidate(
                question_id=102,
                title="Valid Parentheses",
                difficulty=1,
                algorithm_tag="stack",
                knowledge_tags="simulation",
                estimated_minutes=10,
            ),
        ],
        candidate_exams=[
            {
                "exam_id": 9001,
                "title": "Array And Hashing Checkpoint",
                "start_time": now - timedelta(hours=1),
                "end_time": now + timedelta(hours=2),
            }
        ],
    )

    result = build_plan_graph().invoke({"request": request})
    response = result["response"]

    assert response.plan_title
    assert response.current_level == "starter"
    assert response.tasks


def test_plan_graph_repairs_back_to_heuristic_when_llm_verification_fails(monkeypatch):
    from app.services import training_planner  # noqa: WPS433

    monkeypatch.setattr(
        training_planner,
        "_generate_plan_with_llm",
        lambda request, heuristic_plan: {
            "current_level": "advanced",
            "tasks": [
                {
                    "task_type": "question",
                    "question_id": 999999,
                    "title_snapshot": "Invalid task",
                }
            ],
        },
    )

    request = TrainingPlanRequest(
        trace_id="plan-graph-002",
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
            ),
        ],
    )

    result = build_plan_graph().invoke({"request": request})
    response = result["response"]

    assert response.plan_title == "Algorithm Foundation Training Plan"
    assert result["verification_errors"]
