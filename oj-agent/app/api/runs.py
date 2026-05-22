import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.api.serializers import to_api_model
from app.domain.models import CreateRunRequest
from app.services.run_service import run_service


router = APIRouter(prefix="/api/runs", tags=["runs"])


@router.post("")
def create_run(request: CreateRunRequest) -> dict:
    run, bootstrap_artifact = run_service.create_run(request)
    return to_api_model(
        {
            "run_id": run.run_id,
            "status": run.status,
            "entry_graph": run.entry_graph,
            "events_url": f"/api/runs/{run.run_id}/events",
            "artifacts_url": f"/api/runs/{run.run_id}/artifacts",
            "bootstrap_artifact_id": bootstrap_artifact.artifact_id,
        }
    )


@router.get("/{run_id}/events")
def stream_run_events(run_id: str) -> StreamingResponse:
    def event_stream():
        next_seq = 0
        terminal = False
        while not terminal:
            events = run_service.wait_for_events(run_id, next_seq, timeout=15.0)
            if not events:
                run = run_service.get_run(run_id)
                if run.status in {"SUCCEEDED", "FAILED"}:
                    break
                continue

            for event in events:
                payload = json.dumps(
                    to_api_model(event.model_dump(mode="json")),
                    ensure_ascii=False,
                    separators=(",", ":"),
                )
                next_seq = event.seq
                if event.event_type in {"run.completed", "run.failed"}:
                    terminal = True
                yield f"event: run_event\ndata: {payload}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@router.get("/{run_id}/artifacts")
def list_run_artifacts(run_id: str) -> list[dict]:
    return to_api_model([artifact.model_dump(mode="json") for artifact in run_service.list_artifacts(run_id)])
