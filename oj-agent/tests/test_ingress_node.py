from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.nodes.ingress import ingress_node  # noqa: E402
from app.services.conversation_memory import conversation_memory_store  # noqa: E402


def test_ingress_preserves_oj_context_fields():
    result = ingress_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "conversation_id": "c-1",
            "question_id": "1001",
            "question_title": "Two Sum",
            "question_content": "Find two numbers that add up to target.",
            "user_code": "public class Solution {}",
            "judge_result": "WA on sample #2",
            "user_message": "  Why is this WA?  ",
        }
    )

    assert result["question_title"] == "Two Sum"
    assert result["question_content"] == "Find two numbers that add up to target."
    assert result["user_code"] == "public class Solution {}"
    assert result["judge_result"] == "WA on sample #2"
    assert result["user_message"] == "Why is this WA?"


def test_ingress_fills_missing_fields_from_conversation_memory():
    conversation_memory_store.clear()
    conversation_memory_store.remember(
        {
            "trace_id": "trace-000",
            "user_id": "u-1",
            "conversation_id": "c-42",
            "question_id": "1001",
            "question_title": "Two Sum",
            "question_content": "Find two numbers that add up to target.",
            "user_code": "public class Solution {}",
            "judge_result": "WA on sample #2",
            "intent": "analyze_failure",
            "final_answer": "Previous answer",
            "confidence": 0.82,
            "next_action": "Trace the smallest failing example.",
        }
    )

    result = ingress_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "conversation_id": "c-42",
            "user_message": "Can you go deeper on that?",
        }
    )

    assert result["question_title"] == "Two Sum"
    assert result["question_content"] == "Find two numbers that add up to target."
    assert result["user_code"] == "public class Solution {}"
    assert result["judge_result"] == "WA on sample #2"
    assert result["previous_intent"] == "analyze_failure"
    assert result["memory_applied_fields"] == [
        "question_id",
        "question_title",
        "question_content",
        "user_code",
        "judge_result",
    ]
