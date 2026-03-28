from app.graph.edges import (
    ANALYZE_FAILURE,
    ASK_FOR_CONTEXT,
    EXPLAIN_PROBLEM,
    RECOMMEND_QUESTION,
)
from app.graph.nodes.node_utils import append_status
from app.graph.state import AgentState
from app.services.conversation_memory import conversation_memory_store


def _build_continuation_note(state: AgentState) -> list[str]:
    applied_fields = list(state.get("memory_applied_fields") or [])
    if not applied_fields:
        return []
    return [
        "Conversation continuity:",
        "Continuing from remembered context for: " + ", ".join(applied_fields) + ".",
        "",
    ]


def _build_knowledge_note(state: AgentState) -> list[str]:
    knowledge_hits = list(state.get("knowledge_hits") or [])
    if not knowledge_hits:
        return []
    lines = ["Retrieved knowledge snippets:"]
    for hit in knowledge_hits:
        lines.append(f"- {hit['title']}: {hit['excerpt']}")
    lines.append("")
    return lines


def _build_failure_analysis_answer(state: AgentState) -> str:
    diagnosis = state.get("diagnosis") or {
        "observed_signals": [
            f"Question: {state.get('question_title') or 'not provided'}",
            f"Judge result: {state.get('judge_result') or 'not provided'}",
            f"Code status: {'provided' if state.get('user_code') else 'missing'}",
        ],
        "likely_causes": [
            "Compare the failing result with the intended invariant and edge cases first.",
            "Use one concrete failing path to find where expected and actual states diverge.",
        ],
        "verify_steps": [
            "Walk through a minimal failing example by hand.",
            "Check index updates, duplicate handling, and stopping conditions.",
        ],
        "follow_up": "Send the failing input or the smallest suspicious code region if you need a deeper diagnosis.",
    }
    lines = ["Assessment:", "This looks like a failure-analysis request.", ""]
    lines.extend(_build_continuation_note(state))
    lines.extend(_build_knowledge_note(state))
    lines.append("Observed signals:")
    for signal in diagnosis.get("observed_signals", []):
        lines.append(f"- {signal}")

    lines.extend(["", "Likely causes:"])
    for index, cause in enumerate(diagnosis.get("likely_causes", []), start=1):
        lines.append(f"{index}. {cause}")

    lines.extend(["", "What to verify next:"])
    for index, step in enumerate(diagnosis.get("verify_steps", []), start=1):
        lines.append(f"{index}. {step}")

    if diagnosis.get("follow_up"):
        lines.extend(["", "What to send if you are still stuck:", str(diagnosis["follow_up"])])

    lines.extend(
        [
            "",
            "Next prompt you can use:",
            "Here is my failing input and the code path I think is wrong. Can you help me trace it line by line?",
        ]
    )
    lines.extend(["", f"Latest message: {state['user_message']}"])
    return "\n".join(lines)


def _build_problem_explanation_answer(state: AgentState) -> str:
    explanation = state.get("explanation") or {}
    lines = ["Goal:", "This looks like a problem-explanation request.", ""]
    lines.extend(_build_continuation_note(state))
    lines.extend(_build_knowledge_note(state))
    if state.get("question_title"):
        lines.append(f"Question: {state['question_title']}")
    if state.get("question_content"):
        lines.append(f"Problem statement: {state['question_content']}")

    lines.extend(["", "Key details to extract:"])
    for detail in explanation.get("details_to_extract", []):
        lines.append(f"- {detail}")

    lines.extend(["", "Candidate patterns:"])
    for index, pattern in enumerate(explanation.get("candidate_patterns", []), start=1):
        lines.append(f"{index}. {pattern}")

    lines.extend(["", "Implementation plan:"])
    for index, step in enumerate(explanation.get("implementation_plan", []), start=1):
        lines.append(f"{index}. {step}")
    return "\n".join(lines)


def _build_recommendation_answer(state: AgentState) -> str:
    practice_plan = state.get("practice_plan") or {}
    practice_topics = practice_plan.get("topics") or state.get("practice_topics") or []
    lines = [
        "Practice plan:",
        str(practice_plan.get("focus") or "This looks like a recommendation request."),
        "",
    ]
    lines.extend(_build_continuation_note(state))
    lines.extend(_build_knowledge_note(state))
    lines.append("Recommended topics:")

    for index, topic in enumerate(practice_topics, start=1):
        lines.append(f"{index}. {topic['topic']} - {topic['reason']}")
        lines.append(f"   Next step: {topic['next_step']}")

    lines.extend(["", "Recommended search queries:"])
    for query in practice_plan.get("search_queries", []):
        lines.append(f"- {query}")

    lines.extend(["", "Study sequence:"])
    for index, step in enumerate(practice_plan.get("study_sequence", []), start=1):
        lines.append(f"{index}. {step}")

    if practice_plan.get("when_to_move_on"):
        lines.extend(["", f"When to move on: {practice_plan['when_to_move_on']}"])

    question_recommendations = practice_plan.get("question_recommendations", [])
    if question_recommendations:
        lines.extend(["", "Suggested OJ questions:"])
        for item in question_recommendations:
            difficulty = item.get("difficulty")
            difficulty_text = f" (difficulty {difficulty})" if difficulty is not None else ""
            lines.append(f"- [{item['question_id']}] {item['title']}{difficulty_text}")

    if practice_plan.get("checkpoint"):
        lines.extend(["", f"Checkpoint: {practice_plan['checkpoint']}"])
    return "\n".join(lines)


def _build_missing_context_answer(state: AgentState) -> str:
    missing = list(state.get("context_gaps") or [])
    if not missing:
        if not state.get("question_title") and not state.get("question_content"):
            missing.append("question statement")
        if not state.get("user_code"):
            missing.append("code")
        if not state.get("judge_result"):
            missing.append("judge result")
    missing_text = ", ".join(missing) if missing else "more execution details"
    return (
        "LangGraph phase-1 needs more context before giving a specific answer. "
        f"Please provide: {missing_text}."
    )


def _has_problem_context(state: AgentState) -> bool:
    return bool(state.get("question_title") or state.get("question_content"))


def _has_code_context(state: AgentState) -> bool:
    return bool(state.get("user_code"))


def _has_judge_context(state: AgentState) -> bool:
    return bool(state.get("judge_result"))


def _estimate_confidence(state: AgentState, intent: str) -> float:
    base_by_intent = {
        ANALYZE_FAILURE: 0.45,
        EXPLAIN_PROBLEM: 0.5,
        RECOMMEND_QUESTION: 0.55,
        ASK_FOR_CONTEXT: 0.2,
    }
    confidence = base_by_intent[intent]
    if _has_problem_context(state):
        confidence += 0.15
    if _has_code_context(state):
        confidence += 0.15
    if _has_judge_context(state):
        confidence += 0.15
    if intent != ASK_FOR_CONTEXT:
        confidence += 0.05
    return round(min(confidence, 0.95), 2)


def _next_action_for_intent(state: AgentState, intent: str) -> str:
    if intent == ANALYZE_FAILURE:
        diagnosis = state.get("diagnosis") or {}
        steps = diagnosis.get("verify_steps") or []
        if steps:
            return str(steps[0])
        return "Trace the smallest failing example and compare expected vs actual state."
    if intent == EXPLAIN_PROBLEM:
        explanation = state.get("explanation") or {}
        steps = explanation.get("implementation_plan") or []
        if steps:
            return str(steps[0])
        return "Restate the success condition before choosing an algorithm."
    if intent == RECOMMEND_QUESTION:
        practice_plan = state.get("practice_plan") or {}
        sequence = practice_plan.get("study_sequence") or []
        if sequence:
            return str(sequence[0])
        return "Start with the first recommended topic and solve one easy variant."
    return "Send the question statement, current code, and latest judge result."


def finalizer_node(state: AgentState) -> AgentState:
    intent = state.get("intent") or ASK_FOR_CONTEXT
    confidence = _estimate_confidence(state, intent)
    next_action = _next_action_for_intent(state, intent)

    answer_by_intent = {
        ANALYZE_FAILURE: _build_failure_analysis_answer(state),
        RECOMMEND_QUESTION: _build_recommendation_answer(state),
        EXPLAIN_PROBLEM: _build_problem_explanation_answer(state),
        ASK_FOR_CONTEXT: _build_missing_context_answer(state),
    }

    next_state: AgentState = {
        **state,
        "status_events": append_status(
            state,
            "finalizer",
            f"Final response assembled for intent: {intent}.",
        ),
        "confidence": confidence,
        "next_action": next_action,
        "final_answer": answer_by_intent[intent],
    }
    conversation_memory_store.remember(next_state)
    return next_state
