from pydantic import BaseModel, Field


class EvalRecord(BaseModel):
    trace_id: str
    run_id: str
    task_type: str
    graph_name: str
    guardrail_risk: str
    route_names: list[str] = Field(default_factory=list)
    evidence_count: int = 0
