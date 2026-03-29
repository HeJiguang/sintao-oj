def repair_node(state: dict) -> dict:
    return {
        **state,
        "response": state["heuristic_plan"],
    }
