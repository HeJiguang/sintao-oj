def intake_node(state: dict) -> dict:
    return {
        **state,
        "verification_errors": list(state.get("verification_errors") or []),
    }
