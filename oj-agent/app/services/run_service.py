from concurrent.futures import ThreadPoolExecutor

from app.domain.models import ArtifactBody, ArtifactRecord, CreateRunRequest, RunEventRecord, RunRecord
from app.domain.stores import ArtifactStore, EventStore, RunStore, RuntimeStateStore
from app.runtime.context_manager import build_managed_context
from app.runtime.graph import build_runtime_graph
from app.runtime.llm import build_default_llm
from app.runtime.tools import build_default_tool_registry


class RunService:
    """Coordinate the external API contract and the internal runtime graph."""

    def __init__(self) -> None:
        self.runs = RunStore()
        self.events = EventStore()
        self.artifacts = ArtifactStore()
        self.runtime_states = RuntimeStateStore()
        self.tool_registry = build_default_tool_registry()
        self.runtime_graph = build_runtime_graph(
            llm=build_default_llm(),
            tool_registry=self.tool_registry,
        )
        self.executor = ThreadPoolExecutor(max_workers=4, thread_name_prefix="oj-agent-run")

    def create_run(self, request: CreateRunRequest) -> tuple[RunRecord, ArtifactRecord]:
        run = self.runs.save(
            RunRecord(
                run_type=request.run_type,
                source=request.source,
            )
        )
        accepted_snapshot = RunRecord.model_validate(run.model_dump())

        self._append_event(run.run_id, "run.accepted", {"runType": request.run_type, "source": request.source})

        bootstrap = self.artifacts.append(
            ArtifactRecord(
                run_id=run.run_id,
                title="智能体任务已创建",
                summary="本次运行已创建，正在排队执行。",
                render_hint="timeline_card",
                body=ArtifactBody(
                    intent="run_bootstrap",
                    answer=request.context.user_message,
                    next_action="等待智能体开始分析并持续查看事件流。",
                ),
            )
        )

        self.executor.submit(self._execute_run, run.run_id, request)
        return accepted_snapshot, bootstrap

    def list_events(self, run_id: str) -> list[RunEventRecord]:
        return self.events.list_for_run(run_id)

    def wait_for_events(self, run_id: str, after_seq: int, timeout: float | None = None) -> list[RunEventRecord]:
        return self.events.wait_for_new_events(run_id, after_seq, timeout=timeout)

    def list_artifacts(self, run_id: str) -> list[ArtifactRecord]:
        return self.artifacts.list_for_run(run_id)

    def get_run(self, run_id: str) -> RunRecord:
        return self.runs.get(run_id)

    def clear(self) -> None:
        self.runs.clear()
        self.events.clear()
        self.artifacts.clear()
        self.runtime_states.clear()

    def get_runtime_state(self, run_id: str) -> dict | None:
        return self.runtime_states.get(run_id)

    def _execute_run(self, run_id: str, request: CreateRunRequest) -> None:
        run = self.runs.get(run_id)
        try:
            run.status = "RUNNING"
            self.runs.save(run)
            runtime_state = self.runtime_graph.invoke(self._build_initial_state(run, request))
            self._persist_runtime_result(run, runtime_state)
            run.status = "SUCCEEDED"
            self.runs.save(run)
        except Exception as exc:
            run.status = "FAILED"
            self.runs.save(run)
            self._persist_failure(run, exc)

    def _append_event(self, run_id: str, event_type: str, payload: dict) -> None:
        seq = len(self.events.list_for_run(run_id)) + 1
        self.events.append(RunEventRecord(run_id=run_id, seq=seq, event_type=event_type, payload=payload))

    def _build_initial_state(self, run: RunRecord, request: CreateRunRequest) -> dict:
        return {
            "run_id": run.run_id,
            "run_type": request.run_type,
            "status": run.status,
            "managed_context": build_managed_context(
                run_type=request.run_type,
                source=request.source,
                request_context={
                    "question_id": request.context.question_id,
                    "question_title": request.context.question_title,
                    "question_content": request.context.question_content,
                    "user_code": request.context.user_code,
                    "judge_result": request.context.judge_result,
                    "user_message": request.context.user_message,
                },
                tool_catalog=self._build_tool_catalog(),
            ),
            "observations": [],
            "projected_events": [],
            "projected_artifacts": [],
            "errors": [],
            "event_sink": lambda event_type, payload: self._append_event(run.run_id, event_type, payload),
        }

    def _build_tool_catalog(self) -> list[dict]:
        return self.tool_registry.describe()

    def _persist_runtime_result(self, run: RunRecord, runtime_state: dict) -> None:
        sanitized_state = dict(runtime_state)
        sanitized_state.pop("event_sink", None)

        projected_events = list(sanitized_state.get("projected_events", []))
        for artifact_payload in sanitized_state.get("projected_artifacts", []):
            artifact = ArtifactRecord.model_validate(artifact_payload)
            self.artifacts.append(artifact)
            self._append_event(
                run.run_id,
                "artifact.created",
                {"artifactType": artifact.artifact_type, "artifactId": artifact.artifact_id},
            )
            projected_events.append(
                RunEventRecord(
                    run_id=run.run_id,
                    seq=len(projected_events) + 1,
                    event_type="artifact.created",
                    payload={"artifactType": artifact.artifact_type, "artifactId": artifact.artifact_id},
                ).model_dump(mode="json")
            )

        self._append_event(run.run_id, "run.completed", {"status": "SUCCEEDED", "activeNode": "responder_node"})
        projected_events.append(
            RunEventRecord(
                run_id=run.run_id,
                seq=len(projected_events) + 1,
                event_type="run.completed",
                payload={"status": "SUCCEEDED", "activeNode": "responder_node"},
            ).model_dump(mode="json")
        )

        sanitized_state["projected_events"] = projected_events
        self.runtime_states.save(run.run_id, sanitized_state)

    def _persist_failure(self, run: RunRecord, exc: Exception) -> None:
        self.runtime_states.save(
            run.run_id,
            {
                "run_id": run.run_id,
                "status": "FAILED",
                "errors": [str(exc)],
            },
        )
        failure_artifact = self.artifacts.append(
            ArtifactRecord(
                run_id=run.run_id,
                title="运行失败",
                summary="本次请求未能完成。",
                body=ArtifactBody(
                    intent="run_failed",
                    answer=str(exc),
                    next_action="检查模型配置、工具输入或调试页输出后重试。",
                ),
            )
        )
        self._append_event(run.run_id, "run.failed", {"reason": str(exc)})
        self._append_event(
            run.run_id,
            "artifact.created",
            {"artifactType": failure_artifact.artifact_type, "artifactId": failure_artifact.artifact_id},
        )


run_service = RunService()
