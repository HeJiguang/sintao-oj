from app.graph.state import AgentState, StatusEventPayload


def append_status(state: AgentState, node: str, message: str) -> list[StatusEventPayload]:
    events = list(state.get("status_events") or [])
    events.append({"node": node, "message": message})
    return events


def combined_text(*values: str | None) -> str:
    return " ".join(value for value in values if value).lower()
