from app.graph.edges import (
    ANALYZE_FAILURE,
    ASK_FOR_CONTEXT,
    EXPLAIN_PROBLEM,
    RECOMMEND_QUESTION,
)
from app.graph.nodes.node_utils import append_status
from app.graph.state import AgentState


GENERIC_HELP_REQUESTS = {
    "help me look",
    "help me look.",
    "take a look",
    "take a look.",
    "help me analyze",
    "help me analyze.",
    "帮我看看",
    "帮我看一下",
    "看一下",
    "看看",
    "帮我分析",
    "帮我分析一下",
}

RECOMMEND_KEYWORD = "推荐"
EXPLAIN_KEYWORD = "解释"
FAILURE_KEYWORD = "失败"

FAILURE_TERMS = (
    "wa",
    "wrong",
    "wrong answer",
    "tle",
    "time limit",
    "runtime error",
    "compile error",
    "why failed",
    "why is this wrong",
    "debug",
    "为什么错",
    "报错",
    "失败",
    "超时",
    "运行错误",
    "编译错误",
)

RECOMMEND_TERMS = (
    "recommend",
    "practice",
    "similar problems",
    "what should i practice",
    "next problem",
    "next after this",
    "推荐",
    "练什么",
    "练习什么",
    "下一题",
    "下一道题",
)

EXPLAIN_TERMS = (
    "explain",
    "how solve",
    "how to solve",
    "what does this problem mean",
    "解释",
    "讲解",
    "题意",
    "什么意思",
    "怎么做",
)

FOLLOW_UP_TERMS = (
    "go deeper",
    "deeper on that",
    "more detail",
    "more details",
    "explain more",
    "look again",
    "again",
    "what should i verify first",
    "what next",
    "continue",
    "再详细一点",
    "继续",
    "继续分析",
    "展开讲讲",
    "详细讲讲",
)


def detect_intent(
    user_message: str,
    question_title: str | None = None,
    question_content: str | None = None,
    user_code: str | None = None,
    judge_result: str | None = None,
    previous_intent: str | None = None,
) -> str:
    normalized = user_message.strip().lower()

    if not normalized:
        return ASK_FOR_CONTEXT

    if any(term in normalized for term in FAILURE_TERMS) or FAILURE_KEYWORD in normalized:
        return ANALYZE_FAILURE

    if any(term in normalized for term in RECOMMEND_TERMS) or RECOMMEND_KEYWORD in normalized:
        return RECOMMEND_QUESTION

    if any(term in normalized for term in EXPLAIN_TERMS) or EXPLAIN_KEYWORD in normalized:
        return EXPLAIN_PROBLEM

    if previous_intent and any(term in normalized for term in FOLLOW_UP_TERMS):
        return previous_intent

    if normalized in GENERIC_HELP_REQUESTS:
        if judge_result or user_code:
            return ANALYZE_FAILURE
        if question_content or question_title:
            return EXPLAIN_PROBLEM
        return ASK_FOR_CONTEXT

    return ASK_FOR_CONTEXT


def router_node(state: AgentState) -> AgentState:
    intent = detect_intent(
        state["user_message"],
        question_title=state.get("question_title"),
        question_content=state.get("question_content"),
        user_code=state.get("user_code"),
        judge_result=state.get("judge_result"),
        previous_intent=state.get("previous_intent"),
    )
    return {
        **state,
        "intent": intent,
        "status_events": append_status(state, "router", f"Routed request to intent: {intent}."),
    }
