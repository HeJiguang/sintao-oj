from app.graph.state import AgentState
from app.graph.nodes.node_utils import append_status


def _likely_causes(state: AgentState) -> list[str]:
    signal = state.get("judge_signal")
    causes: list[str] = []
    if signal == "wrong_answer":
        causes.extend(
            [
                "A corner case is missing from the current logic.",
                "The implementation may be using the wrong invariant or update order.",
                "Duplicate handling, 0-based vs 1-based indexing, or overflow assumptions may be off.",
            ]
        )
    elif signal == "time_limit":
        causes.extend(
            [
                "The current algorithm is likely slower than the constraint budget allows.",
                "Repeated scans or nested loops may need a precomputed structure or a different pattern.",
            ]
        )
    elif signal == "runtime_error":
        causes.extend(
            [
                "Array bounds, null handling, or divisor safety may be unchecked.",
                "A loop or recursion step may access an invalid state.",
            ]
        )
    elif signal == "compile_error":
        causes.extend(
            [
                "The current code may have a type mismatch, missing import, or invalid syntax.",
                "A helper method or class name may not match the expected signature.",
            ]
        )
    else:
        causes.append("The failure signal is still broad, so the first pass should narrow the failing scenario.")

    if not state.get("user_code"):
        causes.append("The code itself is missing, so this is still a hypothesis-level diagnosis.")
    return causes


def _verify_steps(state: AgentState) -> list[str]:
    signal = state.get("judge_signal")
    steps = [
        "Reproduce the failure on the smallest input that still breaks.",
        "Compare the intended algorithm invariant with the actual variable updates.",
    ]
    if signal == "wrong_answer":
        steps.append("List 2-3 edge cases and walk through the current code by hand.")
    elif signal == "time_limit":
        steps.append("Write down the time complexity of each nested loop or repeated lookup.")
    elif signal == "runtime_error":
        steps.append("Mark every location where an index, pointer, or divisor can become invalid.")
    elif signal == "compile_error":
        steps.append("Start from the first compiler error and ignore downstream errors until it is fixed.")
    else:
        steps.append("Use one failing path to identify exactly where expected and actual states diverge.")
    return steps


def failure_diagnoser_node(state: AgentState) -> AgentState:
    diagnosis = {
        "observed_signals": [
            f"Question: {state.get('question_title') or 'not provided'}",
            f"Judge result: {state.get('judge_result') or 'not provided'}",
            f"Code status: {'provided' if state.get('user_code') else 'missing'}",
        ],
        "likely_causes": _likely_causes(state),
        "verify_steps": _verify_steps(state),
        "follow_up": (
            "If the issue is still unclear, send the exact failing input or the minimal code region around the bug."
        ),
    }

    return {
        **state,
        "diagnosis": diagnosis,
        "status_events": append_status(
            state,
            "failure_diagnoser",
            "Failure diagnosis prepared from judge feedback, code availability, and problem context.",
        ),
    }
