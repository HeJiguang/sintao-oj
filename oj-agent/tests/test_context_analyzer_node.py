from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.nodes.context_analyzer import context_analyzer_node  # noqa: E402


def test_context_analyzer_derives_gaps_topics_and_status():
    result = context_analyzer_node(
        {
            "trace_id": "trace-001",
            "user_id": "u-1",
            "question_title": "Two Sum",
            "judge_result": "WA on sample #2",
            "user_message": "What should I practice next after this problem?",
        }
    )

    assert result["judge_signal"] == "wrong_answer"
    assert "code" in result["context_gaps"]
    assert result["practice_topics"]
    assert result["status_events"][0]["node"] == "context_analyzer"
