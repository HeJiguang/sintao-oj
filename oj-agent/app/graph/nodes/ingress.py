from app.graph.nodes.node_utils import append_status
from app.graph.state import AgentState
from app.services.conversation_memory import REMEMBERED_CONTEXT_FIELDS, conversation_memory_store


def ingress_node(state: AgentState) -> AgentState:
    def _clean(value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = value.strip() # 去除字符串首尾的空格、换行符等
        return cleaned or None

    # 将原始state进行清晰，转化为一个新state
    normalized_state: AgentState = {
        "trace_id": state["trace_id"],
        "user_id": state["user_id"],
        "conversation_id": _clean(state.get("conversation_id")),
        "question_id": _clean(state.get("question_id")),
        "question_title": _clean(state.get("question_title")),
        "question_content": _clean(state.get("question_content")),
        "user_code": _clean(state.get("user_code")),
        "judge_result": _clean(state.get("judge_result")),
        "user_message": state["user_message"].strip(),
        "intent": state.get("intent"),
        "status_events": list(state.get("status_events") or []),
        "final_answer": state.get("final_answer"),
    }

    conversation_snapshot = conversation_memory_store.recall(normalized_state.get("conversation_id"))
    applied_fields: list[str] = []
    for field in REMEMBERED_CONTEXT_FIELDS:
        if normalized_state.get(field) is None and conversation_snapshot.get(field) is not None:
            normalized_state[field] = conversation_snapshot[field]
            applied_fields.append(field)

    normalized_state["memory_applied_fields"] = applied_fields
    normalized_state["previous_intent"] = conversation_snapshot.get("intent")
    normalized_state["previous_next_action"] = conversation_snapshot.get("next_action")
    normalized_state["previous_answer"] = conversation_snapshot.get("final_answer")

    if applied_fields:
        normalized_state["status_events"] = append_status(
            normalized_state,
            "conversation_memory",
            "Loaded remembered context for: " + ", ".join(applied_fields) + ".",
        )

    return normalized_state
