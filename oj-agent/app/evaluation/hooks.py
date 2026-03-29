from app.runtime.models import UnifiedAgentState


def _base_eval_record(state: UnifiedAgentState) -> dict:
    return {
        "trace_id": state.request.trace_id,
        "run_id": state.execution.run_id,
        "task_type": state.request.task_type.value,
        "graph_name": state.execution.graph_name,
        "guardrail_risk": state.guardrail.risk_level.value,
        "route_names": list(state.evidence.route_names),
        "evidence_count": len(state.evidence.items),
    }


def build_chat_eval_record(state: UnifiedAgentState) -> dict:
    record = _base_eval_record(state)
    record["intent"] = state.outcome.intent
    record["has_answer"] = bool(state.outcome.answer)
    return record


def build_plan_eval_record(state: UnifiedAgentState) -> dict:
    record = _base_eval_record(state)
    record["intent"] = state.outcome.intent or "training_plan"
    record["has_write_intents"] = bool(state.outcome.write_intents)
    return record
