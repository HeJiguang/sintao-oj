from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.nodes.router import detect_intent  # noqa: E402


def test_detect_intent_for_failure_analysis():
    assert detect_intent("Why is this WA?") == "analyze_failure"


def test_detect_intent_for_recommendation():
    assert detect_intent("Recommend two binary-search problems.") == "recommend_question"


def test_detect_intent_for_practice_follow_up():
    assert detect_intent("What should I practice next after this problem?") == "recommend_question"


def test_detect_intent_for_problem_explanation():
    assert detect_intent("Explain this problem.") == "explain_problem"


def test_detect_intent_for_missing_context():
    assert detect_intent("Help me look.") == "ask_for_context"
