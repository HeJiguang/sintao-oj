"""Domain 层的内存存储实现。"""

from copy import deepcopy
import threading

from app.domain.models import ArtifactRecord, RunEventRecord, RunRecord


class RunStore:
    """保存 run 主记录的最小存储。"""

    def __init__(self) -> None:
        self._runs: dict[str, RunRecord] = {}
        self._lock = threading.Lock()

    def save(self, run: RunRecord) -> RunRecord:
        with self._lock:
            self._runs[run.run_id] = run
        return run

    def get(self, run_id: str) -> RunRecord:
        with self._lock:
            return self._runs[run_id]

    def clear(self) -> None:
        with self._lock:
            self._runs.clear()


class EventStore:
    """保存事件流，并支持等待新事件。"""

    def __init__(self) -> None:
        self._events: dict[str, list[RunEventRecord]] = {}
        self._condition = threading.Condition()

    def append(self, event: RunEventRecord) -> RunEventRecord:
        with self._condition:
            self._events.setdefault(event.run_id, []).append(event)
            self._condition.notify_all()
        return event

    def list_for_run(self, run_id: str) -> list[RunEventRecord]:
        with self._condition:
            return list(self._events.get(run_id, []))

    def list_after_seq(self, run_id: str, after_seq: int) -> list[RunEventRecord]:
        with self._condition:
            return [event for event in self._events.get(run_id, []) if event.seq > after_seq]

    def wait_for_new_events(self, run_id: str, after_seq: int, timeout: float | None = None) -> list[RunEventRecord]:
        with self._condition:
            existing = [event for event in self._events.get(run_id, []) if event.seq > after_seq]
            if existing:
                return existing
            self._condition.wait(timeout=timeout)
            return [event for event in self._events.get(run_id, []) if event.seq > after_seq]

    def clear(self) -> None:
        with self._condition:
            self._events.clear()
            self._condition.notify_all()


class ArtifactStore:
    """保存最终结果卡片。"""

    def __init__(self) -> None:
        self._artifacts: dict[str, list[ArtifactRecord]] = {}
        self._lock = threading.Lock()

    def append(self, artifact: ArtifactRecord) -> ArtifactRecord:
        with self._lock:
            self._artifacts.setdefault(artifact.run_id, []).append(artifact)
        return artifact

    def list_for_run(self, run_id: str) -> list[ArtifactRecord]:
        with self._lock:
            return list(self._artifacts.get(run_id, []))

    def clear(self) -> None:
        with self._lock:
            self._artifacts.clear()


class RuntimeStateStore:
    """保存每次 run 的最终内部状态快照。"""

    def __init__(self) -> None:
        self._states: dict[str, dict] = {}
        self._lock = threading.Lock()

    def save(self, run_id: str, state: dict) -> None:
        with self._lock:
            self._states[run_id] = deepcopy(state)

    def get(self, run_id: str) -> dict | None:
        with self._lock:
            state = self._states.get(run_id)
        return deepcopy(state) if state is not None else None

    def clear(self) -> None:
        with self._lock:
            self._states.clear()
