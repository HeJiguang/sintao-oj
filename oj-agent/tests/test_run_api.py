import json
import threading
import time

from fastapi.testclient import TestClient

from app.main import app
from app.runtime.contracts import ContextEnvelope, FinalAnswer, PlannerAction
from app.runtime.data_sources import RequestBoundDataSources
from app.runtime.graph import build_runtime_graph
from app.runtime.tools import build_default_tool_registry
from app.services.run_service import run_service


client = TestClient(app)


def wait_until(predicate, timeout: float = 3.0, interval: float = 0.05):
    deadline = time.time() + timeout
    while time.time() < deadline:
        value = predicate()
        if value:
            return value
        time.sleep(interval)
    return predicate()


client = TestClient(app)


class BlockingStreamLLM:
    def __init__(self) -> None:
        self.first_chunk_ready = threading.Event()
        self.release_stream = threading.Event()

    def invoke_structured(self, *, schema, system_prompt: str, user_prompt: str) -> dict:
        del system_prompt
        payload = json.loads(user_prompt.splitlines()[-1])
        managed_context = payload["managed_context"]
        request_context = managed_context["request_context"]
        schema_name = getattr(schema, "__name__", "")

        if schema is ContextEnvelope:
            return {
                "user_goal": "diagnose current failure",
                "inferred_subtask": managed_context["run_type"],
                "wants_full_solution": False,
                "wants_partial_code": True,
                "confidence": 0.91,
                "salient_context": [request_context["question_title"]],
                "missing_context": [],
            }

        if schema is PlannerAction:
            observed_tools = {item["tool_name"] for item in payload.get("observations", [])}
            if "get_question_context" not in observed_tools:
                return {"type": "tool", "tool": "get_question_context", "input": {}, "reason": "Need the problem statement"}
            if "get_workspace_snapshot" not in observed_tools:
                return {"type": "tool", "tool": "get_workspace_snapshot", "input": {}, "reason": "Need the current code"}
            if "get_judge_snapshot" not in observed_tools:
                return {"type": "tool", "tool": "get_judge_snapshot", "input": {}, "reason": "Need the latest judge result"}
            return {"type": "final", "reason": "Enough evidence to start the answer stream"}

        if schema is FinalAnswer:
            return {
                "title": "Streaming diagnosis",
                "summary": "Fallback final answer for non-streaming implementations.",
                "answer": "Fallback full answer.",
                "next_action": "Trace the duplicate-value sample manually.",
                "evidence_refs": ["question_context", "workspace_snapshot", "judge_snapshot"],
                "confidence": 0.87,
                "intent": "analyze_failure",
            }

        if schema_name == "FinalAnswerMeta":
            return {
                "title": "Streaming diagnosis",
                "summary": "The streamed answer pinpoints the hash-map update order bug.",
                "next_action": "Trace the [3,3] sample before changing insertion order.",
                "evidence_refs": ["question_context", "workspace_snapshot", "judge_snapshot"],
                "confidence": 0.87,
                "intent": "analyze_failure",
            }

        raise AssertionError(f"Unhandled schema in BlockingStreamLLM: {schema}")

    def stream_text(self, *, system_prompt: str, user_prompt: str):
        del system_prompt, user_prompt
        self.first_chunk_ready.set()
        yield "First streamed chunk."
        self.release_stream.wait(timeout=2.0)
        yield " Second streamed chunk."


class EmptyKnowledgeRetriever:
    def search(self, *, request_context: dict, query: str | None, question_title: str | None, top_k: int = 3) -> list[dict]:
        del request_context, query, question_title, top_k
        return []


def test_app_boots():
    response = client.get("/healthz")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_debug_playground_page_contains_sse_hooks():
    response = client.get("/debug/agent")

    assert response.status_code == 200
    assert "text/html" in response.headers["content-type"]
    assert "EventSource" in response.text
    assert "/api/runs/" in response.text
    assert "/debug/runs/" in response.text


def test_create_run_returns_required_metadata():
    response = client.post(
        "/api/runs",
        json={
            "runType": "interactive_tutor",
            "source": "workspace_panel",
            "context": {
                "questionId": "1001",
                "questionTitle": "Two Sum",
                "questionContent": "Given an array of integers nums and an integer target...",
                "userCode": "class Solution {}",
                "judgeResult": "WA on sample #2",
                "userMessage": "Give me a hint.",
            },
        },
    )

    assert response.status_code == 200

    payload = response.json()
    assert payload["runId"]
    assert payload["status"] == "ACCEPTED"
    assert payload["entryGraph"] == "oj_tutor_supervisor"
    assert payload["eventsUrl"].endswith("/events")
    assert payload["artifactsUrl"].endswith("/artifacts")
    assert payload["bootstrapArtifactId"]


def test_events_endpoint_streams_sse_blocks():
    create_response = client.post(
        "/api/runs",
        json={
            "runType": "interactive_diagnosis",
            "source": "workspace_panel",
            "context": {
                "questionId": "1001",
                "questionTitle": "Two Sum",
                "questionContent": "Given an array of integers nums and an integer target...",
                "userCode": "class Solution {}",
                "judgeResult": "WA on sample #2",
                "userMessage": "Why is this still wrong?",
            },
        },
    )
    run_id = create_response.json()["runId"]

    event_response = client.get(f"/api/runs/{run_id}/events")

    assert event_response.status_code == 200
    assert "event: run_event" in event_response.text
    assert '"eventType":"run.accepted"' in event_response.text
    assert '"eventType":"message.delta"' in event_response.text
    assert '"eventType":"run.completed"' in event_response.text


def test_run_emits_live_progress_and_message_delta_before_completion():
    llm = BlockingStreamLLM()
    tool_registry = build_default_tool_registry(
        RequestBoundDataSources(
            knowledge_retriever=EmptyKnowledgeRetriever(),
        )
    )
    run_service.tool_registry = tool_registry
    run_service.runtime_graph = build_runtime_graph(llm=llm, tool_registry=tool_registry)
    run_service.clear()

    create_response = client.post(
        "/api/runs",
        json={
            "runType": "interactive_diagnosis",
            "source": "workspace_panel",
            "context": {
                "questionId": "1001",
                "questionTitle": "Two Sum",
                "questionContent": "Given an array of integers nums and an integer target...",
                "userCode": "class Solution {}",
                "judgeResult": "WA on sample #2",
                "userMessage": "Why is this still wrong?",
            },
        },
    )
    run_id = create_response.json()["runId"]

    assert llm.first_chunk_ready.wait(timeout=1.0), "responder never entered streaming mode"

    events = run_service.list_events(run_id)
    event_types = [event.event_type for event in events]

    assert run_service.get_run(run_id).status == "RUNNING"
    assert "graph.node_completed" in event_types
    assert "tool.completed" in event_types
    assert "message.delta" in event_types
    assert "artifact.created" not in event_types
    assert "run.completed" not in event_types

    llm.release_stream.set()
    final_event_types = wait_until(
        lambda: [event.event_type for event in run_service.list_events(run_id)]
        if any(event.event_type == "run.completed" for event in run_service.list_events(run_id))
        else None,
        timeout=3.0,
    )

    assert final_event_types.index("message.delta") < final_event_types.index("artifact.created")
    assert final_event_types.index("artifact.created") < final_event_types.index("run.completed")


def test_artifacts_endpoint_returns_final_answer_card():
    create_response = client.post(
        "/api/runs",
        json={
            "runType": "interactive_recommendation",
            "source": "workspace_panel",
            "context": {
                "questionId": "1001",
                "questionTitle": "Two Sum",
                "questionContent": "Given an array of integers nums and an integer target...",
                "userCode": "class Solution {}",
                "judgeResult": "Accepted",
                "userMessage": "What should I practice next?",
            },
        },
    )
    run_id = create_response.json()["runId"]

    payload = wait_until(lambda: client.get(f"/api/runs/{run_id}/artifacts").json(), timeout=3.0)
    assert len(payload) >= 2
    assert payload[0]["artifactId"]
    assert payload[-1]["artifactType"] == "answer_card"
    assert payload[-1]["body"]["intent"]
    assert payload[-1]["body"]["answer"]
    assert payload[-1]["body"]["nextAction"]


def test_create_run_executes_graph_and_projects_diagnosis_evidence():
    create_response = client.post(
        "/api/runs",
        json={
            "runType": "interactive_diagnosis",
            "source": "workspace_panel",
            "context": {
                "questionId": "1001",
                "questionTitle": "Two Sum",
                "questionContent": "Given an array of integers nums and an integer target...",
                "userCode": "class Solution {}",
                "judgeResult": "WA on sample #2",
                "userMessage": "Help me diagnose the issue.",
            },
        },
    )
    run_id = create_response.json()["runId"]

    payload = wait_until(lambda: client.get(f"/api/runs/{run_id}/artifacts").json(), timeout=3.0)
    evidence = payload[-1]["body"]["evidence"]
    source_ids = {item["sourceId"] for item in evidence}

    assert "question_context" in source_ids
    assert "workspace_snapshot" in source_ids
    assert "judge_snapshot" in source_ids
    assert "knowledge_hash_map" in source_ids


def test_debug_runtime_state_endpoint_returns_final_state_snapshot():
    create_response = client.post(
        "/api/runs",
        json={
            "runType": "interactive_diagnosis",
            "source": "workspace_panel",
            "context": {
                "questionId": "1001",
                "questionTitle": "Two Sum",
                "questionContent": "Given an array of integers nums and an integer target...",
                "userCode": "class Solution {}",
                "judgeResult": "WA on sample #2",
                "userMessage": "Help me diagnose the issue.",
            },
        },
    )
    run_id = create_response.json()["runId"]

    payload = wait_until(lambda: client.get(f"/debug/runs/{run_id}/state").json(), timeout=3.0)
    assert payload["run_id"] == run_id
    assert payload["planner_round"] >= 1
    assert payload["planner_trace"]
    assert payload["final_answer_draft"]["intent"] == "analyze_failure"
