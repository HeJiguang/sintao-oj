from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.schemas.stream_events import ErrorEvent, FinalEvent, MetaEvent, StatusEvent  # noqa: E402
from app.services.stream_emitter import to_sse_event  # noqa: E402


def test_meta_event_serializes_to_sse():
    event = MetaEvent(trace_id="t-1", graph_version="phase-1", mode="demo")
    payload = to_sse_event("meta", event)

    assert payload.startswith("event: meta\n")
    assert '"trace_id":"t-1"' in payload


def test_status_event_serializes_to_sse():
    event = StatusEvent(node="router", message="正在识别问题类型")
    payload = to_sse_event("status", event)

    assert payload.startswith("event: status\n")
    assert '"node":"router"' in payload


def test_final_event_serializes_to_sse():
    event = FinalEvent(answer="done", confidence=0.82, next_action="Try one more edge case by hand.")
    payload = to_sse_event("final", event)

    assert payload.startswith("event: final\n")
    assert '"answer":"done"' in payload
    assert '"next_action":"Try one more edge case by hand."' in payload


def test_error_event_serializes_to_sse():
    event = ErrorEvent(message="boom")
    payload = to_sse_event("error", event)

    assert payload.startswith("event: error\n")
    assert '"message":"boom"' in payload
