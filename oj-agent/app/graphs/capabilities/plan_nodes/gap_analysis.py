from app.services.training_planner import _focus_counter, _infer_level


def gap_analysis_node(state: dict) -> dict:
    request = state["request"]
    weak_counter = _focus_counter(request.recent_submissions)
    focus_tags = [tag for tag, _ in weak_counter.most_common(2)] or ["problem solving rhythm"]
    return {
        **state,
        "inferred_level": _infer_level(request),
        "focus_tags": focus_tags,
    }
