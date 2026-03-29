from app.services import training_planner


def draft_planning_node(state: dict) -> dict:
    request = state["request"]
    heuristic_plan = training_planner._build_heuristic_training_plan(request)
    try:
        llm_payload = training_planner._generate_plan_with_llm(request, heuristic_plan)
    except Exception:
        llm_payload = None
    return {
        **state,
        "heuristic_plan": heuristic_plan,
        "llm_payload": llm_payload,
    }
