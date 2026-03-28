from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.builder import build_graph  # noqa: E402


def test_build_graph_returns_compiled_graph():
    graph = build_graph()

    assert graph is not None
    assert hasattr(graph, "invoke")


def test_graph_invocation_returns_intent_final_answer_and_status_events():
    graph = build_graph()

    result = graph.invoke(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "conversation_id": "c-1",
            "question_id": "1001",
            "question_title": "Two Sum",
            "judge_result": "WA on sample #2",
            "user_code": "public class Solution {}",
            "user_message": "Why is this WA?",
        }
    )

    assert result["intent"] == "analyze_failure"
    assert "Assessment:" in result["final_answer"]
    assert "Likely causes:" in result["final_answer"]
    assert result["status_events"][0]["node"] == "context_analyzer"
    assert any(event["node"] == "failure_diagnoser" for event in result["status_events"])
    assert result["confidence"] >= 0.8
    assert result["next_action"]
