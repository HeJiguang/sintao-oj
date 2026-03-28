from collections.abc import Iterator, Mapping
import json
from typing import Any

from app.llm.base import CHAT_CAPABILITY
from app.llm.factory import build_llm_client
from app.services.conversation_memory import conversation_memory_store


def generate_chat_answer(state: Mapping[str, Any]) -> tuple[str, float, str, str]:
    fallback_answer = str(state.get("final_answer") or "I need more context before I can help precisely.")
    fallback_confidence = float(state.get("confidence") or 0.2)
    fallback_next_action = str(state.get("next_action") or "Send the question statement, your code, and the latest judge result.")

    llm_client = build_llm_client()
    if not llm_client.is_available():
        remember_generated_answer(state, fallback_answer, fallback_confidence, fallback_next_action)
        return fallback_answer, fallback_confidence, fallback_next_action, llm_client.model_name(CHAT_CAPABILITY)

    system_prompt, user_prompt = build_chat_prompts(state)
    try:
        answer = llm_client.generate_text(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            capability=CHAT_CAPABILITY,
        ).strip()
        if not answer:
            raise RuntimeError("llm returned an empty chat answer")
        remember_generated_answer(state, answer, fallback_confidence, fallback_next_action)
        return answer, fallback_confidence, fallback_next_action, llm_client.model_name(CHAT_CAPABILITY)
    except Exception:
        remember_generated_answer(state, fallback_answer, fallback_confidence, fallback_next_action)
        return fallback_answer, fallback_confidence, fallback_next_action, "heuristic-fallback"


def stream_chat_answer(state: Mapping[str, Any]) -> Iterator[str]:
    llm_client = build_llm_client()
    if not llm_client.is_available():
        fallback_answer = str(state.get("final_answer") or "")
        if fallback_answer:
            yield fallback_answer
        return

    system_prompt, user_prompt = build_chat_prompts(state)
    yield from llm_client.stream_text(
        system_prompt=system_prompt,
        user_prompt=user_prompt,
        capability=CHAT_CAPABILITY,
    )


def remember_generated_answer(
    state: Mapping[str, Any],
    answer: str,
    confidence: float,
    next_action: str,
) -> None:
    conversation_memory_store.remember(
        {
            **dict(state),
            "final_answer": answer,
            "confidence": confidence,
            "next_action": next_action,
        }
    )


def build_chat_prompts(state: Mapping[str, Any]) -> tuple[str, str]:
    system_prompt = "\n".join(
        [
            "You are SynCode's coding assistant inside an online judge workspace.",
            "Be concise, specific, and actionable.",
            "Prefer explaining the next debugging or solving step over giving generic theory.",
            "If code or judge context is present, use it directly.",
            "If retrieved knowledge snippets are present, ground your answer in them instead of inventing external references.",
        ]
    )
    heuristic_context = _heuristic_context(state)
    retrieved_knowledge = _retrieved_knowledge_block(state)
    user_prompt = "\n".join(
        [
            f"Intent: {state.get('intent') or 'unknown'}",
            f"Question title: {state.get('question_title') or 'not provided'}",
            f"Question content: {state.get('question_content') or 'not provided'}",
            f"User code: {state.get('user_code') or 'not provided'}",
            f"Judge result: {state.get('judge_result') or 'not provided'}",
            f"Latest user message: {state.get('user_message') or 'not provided'}",
            "",
            "Retrieved knowledge:",
            retrieved_knowledge,
            "",
            "Heuristic context:",
            heuristic_context,
        ]
    )
    return system_prompt, user_prompt


def _heuristic_context(state: Mapping[str, Any]) -> str:
    payload = {
        "diagnosis": state.get("diagnosis"),
        "explanation": state.get("explanation"),
        "practice_plan": state.get("practice_plan"),
        "context_gaps": state.get("context_gaps"),
        "previous_intent": state.get("previous_intent"),
    }
    return json.dumps(payload, ensure_ascii=False)


def _retrieved_knowledge_block(state: Mapping[str, Any]) -> str:
    knowledge_hits = list(state.get("knowledge_hits") or [])
    if not knowledge_hits:
        return "No retrieved knowledge snippet matched."

    lines = []
    for hit in knowledge_hits:
        lines.append(f"- {hit.get('title')}: {hit.get('excerpt')}")
    return "\n".join(lines)
