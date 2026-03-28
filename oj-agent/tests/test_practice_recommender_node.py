from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.edges import RECOMMEND_QUESTION  # noqa: E402
from app.graph.nodes import practice_recommender  # noqa: E402
from app.graph.nodes.practice_recommender import practice_recommender_node  # noqa: E402


def test_practice_recommender_generates_ranked_topics_and_queries():
    result = practice_recommender_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "question_title": "Two Sum",
            "question_content": "Find two numbers that add up to target.",
            "user_message": "What should I practice next after this problem?",
            "intent": RECOMMEND_QUESTION,
            "practice_topics": [
                {
                    "topic": "Hash map lookups",
                    "reason": "Two-sum style target matching uses complement checks.",
                    "next_step": "Practice array complement lookup problems.",
                }
            ],
            "status_events": [],
        }
    )

    assert result["practice_plan"]["topics"][0]["topic"] == "Hash map lookups"
    assert result["practice_plan"]["search_queries"]
    assert result["practice_plan"]["study_sequence"]
    assert result["practice_plan"]["when_to_move_on"]
    assert result["status_events"][-1]["node"] == "practice_recommender"


def test_practice_recommender_includes_catalog_recommendations(monkeypatch):
    monkeypatch.setattr(
        practice_recommender,
        "fetch_questions_for_queries",
        lambda queries, limit=3: [
            {
                "question_id": "1001",
                "title": "Two Sum",
                "difficulty": 1,
                "source_query": queries[0],
            },
            {
                "question_id": "1010",
                "title": "Contains Duplicate",
                "difficulty": 1,
                "source_query": queries[0],
            },
        ],
    )

    result = practice_recommender_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "question_title": "Two Sum",
            "question_content": "Find two numbers that add up to target.",
            "user_message": "What should I practice next after this problem?",
            "intent": RECOMMEND_QUESTION,
            "practice_topics": [
                {
                    "topic": "Hash map lookups",
                    "reason": "Two-sum style target matching uses complement checks.",
                    "next_step": "Practice array complement lookup problems.",
                }
            ],
            "status_events": [],
        }
    )

    assert result["practice_plan"]["question_recommendations"][0]["title"] == "Two Sum"
    assert result["practice_plan"]["question_recommendations"][1]["title"] == "Contains Duplicate"
