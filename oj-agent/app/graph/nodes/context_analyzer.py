from app.graph.state import AgentState, PracticeTopic
from app.graph.nodes.node_utils import append_status, combined_text


TOPIC_RULES = [
    (
        ("two sum", "target", "array"),
        {
            "topic": "Hash map lookups",
            "reason": "Target-matching array problems often hinge on complement checks.",
            "next_step": "Practice array problems where you convert repeated scans into one-pass lookups.",
        },
    ),
    (
        ("sorted", "two pointers", "pointer"),
        {
            "topic": "Two pointers",
            "reason": "Sorted array questions often become cleaner with inward pointer movement.",
            "next_step": "Practice pair and window problems on sorted inputs.",
        },
    ),
    (
        ("binary search",),
        {
            "topic": "Binary search invariants",
            "reason": "Binary-search bugs usually come from interval definitions and boundary updates.",
            "next_step": "Practice lower-bound and answer-space binary search variants.",
        },
    ),
    (
        ("subarray", "substring", "window"),
        {
            "topic": "Sliding window and prefix sums",
            "reason": "Contiguous range problems often reduce to window maintenance or prefix accumulation.",
            "next_step": "Practice fixed-size and variable-size range problems.",
        },
    ),
    (
        ("tree",),
        {
            "topic": "Tree traversal",
            "reason": "Tree problems usually depend on DFS/BFS order and what state is carried between nodes.",
            "next_step": "Practice recursive DFS and iterative BFS tree templates.",
        },
    ),
    (
        ("graph", "shortest path", "bfs", "dfs"),
        {
            "topic": "Graph traversal",
            "reason": "Graph questions depend on choosing the right traversal or shortest-path routine.",
            "next_step": "Practice BFS reachability and Dijkstra-style weighted graph problems.",
        },
    ),
    (
        ("dp", "dynamic programming"),
        {
            "topic": "Dynamic programming state design",
            "reason": "DP questions are easier once the state, transition, and base cases are explicit.",
            "next_step": "Practice writing state definitions before coding transitions.",
        },
    ),
]


def detect_judge_signal(judge_result: str | None) -> str | None:
    if not judge_result:
        return None
    normalized = judge_result.lower()
    if "wa" in normalized or "wrong answer" in normalized:
        return "wrong_answer"
    if "tle" in normalized or "time limit" in normalized:
        return "time_limit"
    if "mle" in normalized or "memory limit" in normalized:
        return "memory_limit"
    if "re" in normalized or "runtime error" in normalized:
        return "runtime_error"
    if "ce" in normalized or "compile error" in normalized:
        return "compile_error"
    return "judge_feedback"


def infer_practice_topics(state: AgentState) -> list[PracticeTopic]:
    text = combined_text(
        state.get("question_title"),
        state.get("question_content"),
        state.get("judge_result"),
        state.get("user_message"),
    )
    topics: list[PracticeTopic] = []
    seen: set[str] = set()

    for triggers, topic in TOPIC_RULES:
        if any(trigger in text for trigger in triggers) and topic["topic"] not in seen:
            topics.append(topic)
            seen.add(topic["topic"])

    judge_signal = detect_judge_signal(state.get("judge_result"))
    if judge_signal == "wrong_answer" and "Edge-case debugging" not in seen:
        topics.append(
            {
                "topic": "Edge-case debugging",
                "reason": "Wrong-answer failures usually hide in missing corner cases or invalid assumptions.",
                "next_step": "Practice writing minimal counterexamples before changing code.",
            }
        )
        seen.add("Edge-case debugging")
    if judge_signal == "time_limit" and "Complexity optimization" not in seen:
        topics.append(
            {
                "topic": "Complexity optimization",
                "reason": "Time-limit failures usually mean the chosen algorithm is asymptotically too slow.",
                "next_step": "Compare your current complexity with the expected constraint range.",
            }
        )
        seen.add("Complexity optimization")
    if judge_signal == "runtime_error" and "Defensive implementation checks" not in seen:
        topics.append(
            {
                "topic": "Defensive implementation checks",
                "reason": "Runtime errors often come from invalid indices, null access, or unsafe arithmetic.",
                "next_step": "Practice tracing array bounds, pointer validity, and divisor safety.",
            }
        )
        seen.add("Defensive implementation checks")
    if judge_signal == "compile_error" and "Compile-time hygiene" not in seen:
        topics.append(
            {
                "topic": "Compile-time hygiene",
                "reason": "Compile errors come from mismatched types, missing imports, or invalid syntax.",
                "next_step": "Practice reducing the failure to the first uncompilable line and its dependency.",
            }
        )
        seen.add("Compile-time hygiene")

    if not topics:
        topics.append(
            {
                "topic": "Problem classification",
                "reason": "You still need a reliable way to map the prompt to a known algorithm family.",
                "next_step": "Practice identifying input shape, constraints, and target operations before coding.",
            }
        )

    return topics[:3]


def detect_context_gaps(state: AgentState) -> list[str]:
    gaps = []
    if not state.get("question_title") and not state.get("question_content"):
        gaps.append("question statement")
    if not state.get("user_code"):
        gaps.append("code")
    if not state.get("judge_result"):
        gaps.append("judge result")
    return gaps


def context_analyzer_node(state: AgentState) -> AgentState:
    context_gaps = detect_context_gaps(state)
    judge_signal = detect_judge_signal(state.get("judge_result"))
    practice_topics = infer_practice_topics(state)

    available = []
    if state.get("question_title") or state.get("question_content"):
        available.append("problem")
    if state.get("user_code"):
        available.append("code")
    if state.get("judge_result"):
        available.append("judge result")
    if not available:
        available.append("user message only")

    message = "Context ready from: " + ", ".join(available)
    if judge_signal:
        message += f" (judge signal: {judge_signal})"

    return {
        **state,
        "context_gaps": context_gaps,
        "judge_signal": judge_signal,
        "practice_topics": practice_topics,
        "status_events": append_status(state, "context_analyzer", message),
    }
