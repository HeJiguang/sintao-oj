from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.edges import ANALYZE_FAILURE, ASK_FOR_CONTEXT, EXPLAIN_PROBLEM, RECOMMEND_QUESTION  # noqa: E402
from app.graph.nodes.finalizer import finalizer_node  # noqa: E402


def test_finalizer_uses_failure_context_in_answer():
    result = finalizer_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "question_title": "Two Sum",
            "judge_result": "WA on sample #2",
            "user_code": "public class Solution {}",
            "user_message": "Why is this WA?",
            "intent": ANALYZE_FAILURE,
        }
    )

    assert "Two Sum" in result["final_answer"]
    assert "WA on sample #2" in result["final_answer"]
    assert "code" in result["final_answer"].lower()
    assert "Assessment:" in result["final_answer"]
    assert "Observed signals:" in result["final_answer"]
    assert "Likely causes:" in result["final_answer"]
    assert "What to verify next:" in result["final_answer"]
    assert "Next prompt you can use:" in result["final_answer"]
    assert result["confidence"] >= 0.8
    assert result["next_action"]


def test_finalizer_uses_problem_context_in_answer():
    result = finalizer_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "question_title": "Two Sum",
            "question_content": "Find two numbers that add up to target.",
            "user_message": "Explain the problem.",
            "intent": EXPLAIN_PROBLEM,
        }
    )

    assert "Two Sum" in result["final_answer"]
    assert "Find two numbers that add up to target." in result["final_answer"]
    assert "Goal:" in result["final_answer"]
    assert "Candidate patterns:" in result["final_answer"]
    assert "Implementation plan:" in result["final_answer"]
    assert result["confidence"] >= 0.6
    assert "Restate the success condition" in result["next_action"]


def test_finalizer_builds_practice_recommendation_answer():
    result = finalizer_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "question_title": "Two Sum",
            "user_message": "What should I practice next?",
            "intent": RECOMMEND_QUESTION,
            "practice_topics": [
                {
                    "topic": "Hash map lookups",
                    "reason": "Two-sum style target matching usually needs constant-time complement checks.",
                    "next_step": "Practice 2-3 array + hash map problems with duplicate handling.",
                }
            ],
            "practice_plan": {
                "focus": "Turn the current problem into a repeatable practice plan.",
                "topics": [
                    {
                        "topic": "Hash map lookups",
                        "reason": "Two-sum style target matching usually needs constant-time complement checks.",
                        "next_step": "Practice 2-3 array + hash map problems with duplicate handling.",
                    }
                ],
                "search_queries": ["OJ Hash map lookups"],
                "study_sequence": ["Reinforce the current pattern with Hash map lookups."],
                "when_to_move_on": "Move to the next topic once you can explain the pattern choice and solve one variant without hints.",
                "question_recommendations": [
                    {"question_id": "1001", "title": "Two Sum", "difficulty": 1, "source_query": "OJ Hash map lookups"}
                ],
            },
        }
    )

    assert "Practice plan:" in result["final_answer"]
    assert "Hash map lookups" in result["final_answer"]
    assert "Recommended search queries:" in result["final_answer"]
    assert "Study sequence:" in result["final_answer"]
    assert "Suggested OJ questions:" in result["final_answer"]
    assert "Two Sum" in result["final_answer"]
    assert result["confidence"] >= 0.7
    assert result["next_action"]


def test_finalizer_lists_missing_context_fields():
    result = finalizer_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "user_message": "Help me look.",
            "intent": ASK_FOR_CONTEXT,
        }
    )

    assert "question" in result["final_answer"].lower()
    assert "code" in result["final_answer"].lower()
    assert "Please provide:" in result["final_answer"]
    assert result["confidence"] < 0.5
