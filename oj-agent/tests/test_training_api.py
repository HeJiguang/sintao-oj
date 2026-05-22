from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_training_plan_endpoint_returns_compatible_plan_shape():
    response = client.post(
        "/api/training/plan",
        json={
            "trace_id": "trace-1",
            "user_id": 1001,
            "current_level": "starter",
            "target_direction": "algorithm_foundation",
            "preferred_count": 2,
            "recent_submissions": [
                {
                    "submit_id": 11,
                    "question_id": 2001,
                    "title": "Two Sum",
                    "difficulty": 1,
                    "algorithm_tag": "hash_map",
                    "knowledge_tags": "array,hash",
                    "pass": 0,
                    "score": 40,
                    "exe_message": "Wrong Answer",
                }
            ],
            "candidate_questions": [
                {
                    "question_id": 2001,
                    "title": "Two Sum",
                    "difficulty": 1,
                    "algorithm_tag": "hash_map",
                    "knowledge_tags": "array,hash",
                    "estimated_minutes": 20,
                },
                {
                    "question_id": 2002,
                    "title": "Valid Parentheses",
                    "difficulty": 1,
                    "algorithm_tag": "stack",
                    "knowledge_tags": "stack,string",
                    "estimated_minutes": 25,
                },
            ],
            "candidate_exams": [
                {
                    "exam_id": 3001,
                    "title": "Stage Test",
                    "start_time": "2026-04-11T10:00:00",
                    "end_time": "2026-04-11T12:00:00",
                }
            ],
        },
    )

    assert response.status_code == 200

    payload = response.json()
    assert payload["current_level"] == "starter"
    assert payload["target_direction"] == "algorithm_foundation"
    assert payload["plan_title"]
    assert payload["plan_goal"]
    assert payload["ai_summary"]
    assert len(payload["tasks"]) == 2
    assert payload["tasks"][0]["task_type"] == "question"
    assert payload["tasks"][0]["question_id"] == 2001
    assert payload["tasks"][0]["title_snapshot"] == "Two Sum"
    assert payload["tasks"][1]["task_type"] == "test"
    assert payload["tasks"][1]["exam_id"] == 3001
    assert payload["tasks"][1]["title_snapshot"] == "Stage Test"
