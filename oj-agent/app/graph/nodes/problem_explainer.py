from app.graph.state import AgentState
from app.graph.nodes.node_utils import append_status, combined_text


def _candidate_patterns(state: AgentState) -> list[str]:
    text = combined_text(state.get("question_title"), state.get("question_content"))
    patterns: list[str] = []
    if "two sum" in text or "target" in text:
        patterns.extend(
            [
                "Hash map complements when the input is unsorted.",
                "Two pointers if the data can be sorted or is already sorted.",
            ]
        )
    if "subarray" in text or "substring" in text:
        patterns.extend(
            [
                "Sliding window if the condition can be maintained incrementally.",
                "Prefix sums if range totals need fast recomputation.",
            ]
        )
    if "tree" in text:
        patterns.append("DFS or BFS traversal with clear per-node state.")
    if "graph" in text or "shortest path" in text:
        patterns.append("BFS for unweighted graphs or Dijkstra for weighted shortest paths.")
    if "dp" in text or "dynamic programming" in text:
        patterns.append("DP once the state, transition, and base case are explicit.")
    if not patterns:
        patterns.append("Start by identifying the data structure operations that dominate the solution.")
    return patterns[:3]


def problem_explainer_node(state: AgentState) -> AgentState:
    explanation = {
        "goal": state.get("question_title") or "Understand the problem and map it to a solution pattern.",
        "details_to_extract": [
            "What the input represents and what the output must guarantee.",
            "The constraint range that decides whether brute force is acceptable.",
            "Any ordering, uniqueness, or contiguous-substructure requirement in the statement.",
        ],
        "candidate_patterns": _candidate_patterns(state),
        "implementation_plan": [
            "Restate the success condition in one sentence before coding.",
            "Pick the simplest pattern that satisfies the constraints.",
            "Validate the plan on a tiny hand-worked example before implementation.",
        ],
    }

    return {
        **state,
        "explanation": explanation,
        "status_events": append_status(
            state,
            "problem_explainer",
            "Problem explanation prepared with candidate patterns and an implementation outline.",
        ),
    }
