from app.services import training_planner


def verification_node(state: dict) -> dict:
    llm_payload = state.get("llm_payload")
    if llm_payload is None:
        return {
            **state,
            "response": state["heuristic_plan"],
            "verification_errors": [],
        }

    try:
        response = training_planner._normalize_llm_plan(
            state["request"],
            state["heuristic_plan"],
            llm_payload,
        )
        return {
            **state,
            "response": response,
            "verification_errors": [],
        }
    except Exception as exc:
        return {
            **state,
            "verification_errors": [str(exc)],
        }
