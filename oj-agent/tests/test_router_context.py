from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.nodes.router import EXPLAIN_KEYWORD, RECOMMEND_KEYWORD, detect_intent  # noqa: E402


def test_detect_intent_uses_judge_result_for_generic_help_request():
    assert detect_intent("Help me look.", judge_result="WA on sample #2") == "analyze_failure"


def test_detect_intent_uses_problem_context_for_generic_help_request():
    assert (
        detect_intent("Help me look.", question_content="Find two numbers that add up to target.")
        == "explain_problem"
    )


def test_recommend_keyword_matches_direct_chinese_prompt():
    assert RECOMMEND_KEYWORD in "可以推荐我下一道练习题吗？"
    assert detect_intent("可以推荐我下一道练习题吗？") == "recommend_question"


def test_explain_keyword_matches_direct_chinese_prompt():
    assert EXPLAIN_KEYWORD in "你可以解释一下这道题吗？"
    assert detect_intent("你可以解释一下这道题吗？") == "explain_problem"


def test_detect_intent_reuses_previous_intent_for_follow_up_message():
    assert (
        detect_intent(
            "Can you go deeper on that?",
            previous_intent="analyze_failure",
            judge_result="WA on sample #2",
        )
        == "analyze_failure"
    )
