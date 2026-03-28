import json
from pydantic import BaseModel


def to_sse_event(event_name: str, payload: BaseModel) -> str:
    return f"event: {event_name}\ndata: {json.dumps(payload.model_dump(), ensure_ascii=False, separators=(',', ':'))}\n\n"
