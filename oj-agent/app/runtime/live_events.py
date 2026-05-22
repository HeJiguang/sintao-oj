from collections.abc import Callable
from typing import Any

from app.domain.models import RunEventRecord


EventSink = Callable[[str, dict[str, Any]], None]


def append_projected_event(
    state: dict,
    event_type: str,
    payload: dict[str, Any],
    *,
    broadcast: bool = True,
) -> list[dict]:
    projected_events = list(state.get("projected_events", []))
    event = RunEventRecord(
        run_id=state["run_id"],
        seq=len(projected_events) + 1,
        event_type=event_type,
        payload=payload,
    )
    projected_events.append(event.model_dump(mode="json"))

    sink = state.get("event_sink")
    if broadcast and callable(sink):
        sink(event_type, payload)

    return projected_events
