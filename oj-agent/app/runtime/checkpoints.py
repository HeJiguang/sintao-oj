from pydantic import BaseModel

from app.runtime.models import UnifiedAgentState


class CheckpointPayload(BaseModel):
    checkpoint_id: str
    run_id: str
    graph_name: str
    node_name: str
    sequence_no: int
    state: UnifiedAgentState
