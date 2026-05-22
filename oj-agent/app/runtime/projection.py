from app.domain.models import ArtifactBody, ArtifactRecord, EvidenceItem, RunEventRecord


def project_artifact(state: dict) -> ArtifactRecord:
    """把最终回答投影成前端真正渲染的 artifact。"""
    final = state["final_answer_draft"]
    evidence_lookup = _collect_evidence(state)
    evidence = [evidence_lookup[item] for item in final.get("evidence_refs", []) if item in evidence_lookup]
    return ArtifactRecord(
        run_id=state["run_id"],
        artifact_type="answer_card",
        title=final["title"],
        summary=final["summary"],
        body=ArtifactBody(
            intent=final["intent"],
            answer=final["answer"],
            next_action=final["next_action"],
            evidence=evidence,
        ),
    )


def project_events(state: dict, artifact: ArtifactRecord) -> list[RunEventRecord]:
    """把内部运行结果投影成前端可消费的事件流。"""
    events: list[RunEventRecord] = []
    next_seq = 1

    events.append(
        RunEventRecord(
            run_id=state["run_id"],
            seq=next_seq,
            event_type="run.started",
            payload={"activeNode": "context_node"},
        )
    )
    next_seq += 1

    events.append(
        RunEventRecord(
            run_id=state["run_id"],
            seq=next_seq,
            event_type="graph.node_completed",
            payload={"node": "context_node", "graphName": "oj_tutor_supervisor"},
        )
    )
    next_seq += 1

    observations = list(state.get("observations", []))
    for round_index, action in enumerate(state.get("planner_trace", []), start=1):
        events.append(
            RunEventRecord(
                run_id=state["run_id"],
                seq=next_seq,
                event_type="graph.node_completed",
                payload={
                    "node": "planner_node",
                    "graphName": "oj_tutor_supervisor",
                    "plannerRound": round_index,
                    "actionType": action["type"],
                    "tool": action.get("tool"),
                    "reason": action.get("reason"),
                },
            )
        )
        next_seq += 1

        if action["type"] == "tool" and round_index <= len(observations):
            observation = observations[round_index - 1]
            events.append(
                RunEventRecord(
                    run_id=state["run_id"],
                    seq=next_seq,
                    event_type="tool.completed" if observation.get("ok", True) else "tool.failed",
                    payload={
                        "node": "tool_node",
                        "tool": observation["tool_name"],
                        "ok": observation.get("ok", True),
                        "error": observation.get("error"),
                    },
                )
            )
            next_seq += 1

    evidence_lookup = _collect_evidence(state)
    if evidence_lookup:
        events.append(
            RunEventRecord(
                run_id=state["run_id"],
                seq=next_seq,
                event_type="retrieval.evidence_ready",
                payload={
                    "graphName": "oj_tutor_supervisor",
                    "evidenceCount": len(evidence_lookup),
                    "sources": list(evidence_lookup.keys()),
                },
            )
        )
        next_seq += 1

    for chunk in _split_answer(state["final_answer_draft"]["answer"]):
        events.append(
            RunEventRecord(
                run_id=state["run_id"],
                seq=next_seq,
                event_type="message.delta",
                payload={"delta": chunk},
            )
        )
        next_seq += 1

    events.append(
        RunEventRecord(
            run_id=state["run_id"],
            seq=next_seq,
            event_type="graph.node_completed",
            payload={"node": "responder_node", "graphName": "oj_tutor_supervisor"},
        )
    )
    next_seq += 1

    events.append(
        RunEventRecord(
            run_id=state["run_id"],
            seq=next_seq,
            event_type="artifact.created",
            payload={"artifactType": artifact.artifact_type, "artifactId": artifact.artifact_id},
        )
    )
    next_seq += 1

    events.append(
        RunEventRecord(
            run_id=state["run_id"],
            seq=next_seq,
            event_type="run.completed",
            payload={"status": "SUCCEEDED", "activeNode": "responder_node"},
        )
    )
    return events


def _collect_evidence(state: dict) -> dict[str, EvidenceItem]:
    """把 observations 转成最终回答里可展示的证据字典。"""
    lookup: dict[str, EvidenceItem] = {}
    for result in state.get("observations", []):
        if not result.get("ok", True):
            continue
        tool_name = result["tool_name"]
        payload = result["payload"]
        if tool_name == "get_question_context":
            lookup["question_context"] = EvidenceItem(
                source_id="question_context",
                title=payload.get("title") or "题目上下文",
                snippet=(payload.get("content") or "")[:160],
            )
        elif tool_name == "get_workspace_snapshot":
            lookup["workspace_snapshot"] = EvidenceItem(
                source_id="workspace_snapshot",
                title="工作区代码",
                snippet=(payload.get("user_code") or "")[:160],
            )
        elif tool_name == "get_judge_snapshot":
            lookup["judge_snapshot"] = EvidenceItem(
                source_id="judge_snapshot",
                title="最近一次判题结果",
                snippet=payload.get("raw_result") or "当前没有可用的判题信息。",
            )
        elif tool_name == "retrieve_knowledge_evidence":
            for index, item in enumerate(payload.get("items", []), start=1):
                lookup[f"knowledge_{index}"] = EvidenceItem(
                    source_id=item["source_id"],
                    title=item["title"],
                    snippet=item["snippet"],
                )
    return lookup


def _split_answer(answer: str, chunk_size: int = 18) -> list[str]:
    """把最终答案切成小块，供 SSE 的 `message.delta` 逐段输出。"""
    return [answer[index : index + chunk_size] for index in range(0, len(answer), chunk_size)]
