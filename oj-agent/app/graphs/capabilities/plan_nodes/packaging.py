def packaging_node(state: dict) -> dict:
    if state.get("response") is not None:
        return state
    return {
        **state,
        "response": state["heuristic_plan"],
    }
