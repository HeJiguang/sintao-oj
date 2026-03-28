from pydantic import BaseModel, Field


class MetaEvent(BaseModel):
    trace_id: str
    graph_version: str
    mode: str


class StatusEvent(BaseModel):
    node: str
    message: str


class DeltaEvent(BaseModel):
    answer: str


class FinalEvent(BaseModel):
    answer: str
    confidence: float = Field(ge=0.0, le=1.0)
    next_action: str | None = None


class ErrorEvent(BaseModel):
    message: str
