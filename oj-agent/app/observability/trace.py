from pydantic import BaseModel


class RunTrace(BaseModel):
    trace_id: str
    run_id: str
    graph_name: str
    task_type: str
    user_id: str


class NodeTraceEvent(BaseModel):
    trace_id: str
    run_id: str
    graph_name: str
    node_name: str
    status: str


class InMemoryTraceStore:
    def __init__(self) -> None:
        self._runs: dict[str, RunTrace] = {}
        self._node_events: dict[str, list[NodeTraceEvent]] = {}

    def record_run(self, run_trace: RunTrace) -> None:
        self._runs[run_trace.run_id] = run_trace

    def record_node_event(self, event: NodeTraceEvent) -> None:
        self._node_events.setdefault(event.run_id, []).append(event)

    def get_run(self, run_id: str) -> RunTrace:
        return self._runs[run_id]

    def list_node_events(self, run_id: str) -> list[NodeTraceEvent]:
        return list(self._node_events.get(run_id, []))

    def clear(self) -> None:
        self._runs.clear()
        self._node_events.clear()


trace_store = InMemoryTraceStore()
