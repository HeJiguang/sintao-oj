import json

from app.runtime.contracts import FinalAnswer, FinalAnswerMeta
from app.runtime.live_events import append_projected_event
from app.runtime.projection import project_artifact


ANSWER_SYSTEM_PROMPT = (
    "You are the OnlineOJ responder node. "
    "Write only the final answer body shown to the learner. "
    "Do not output JSON, titles, summaries, or markdown front matter. "
    "Teach first, avoid giving a direct submission-ready full solution unless the user explicitly asked for it."
)

METADATA_SYSTEM_PROMPT = (
    "You are the OnlineOJ responder metadata node. "
    "Given the already-streamed final answer body, produce only the structured card metadata."
)

FALLBACK_SYSTEM_PROMPT = (
    "You are the OnlineOJ responder node. "
    "Produce the full structured final answer when streaming is unavailable."
)


def build_responder_node(llm):
    """Stream the final answer body, then synthesize the final artifact metadata."""

    def node(state: dict) -> dict:
        stream_user_prompt = "\n".join(
            [
                "Write the final answer body for the user.",
                json.dumps(
                    {
                        "managed_context": state["managed_context"],
                        "interpretation": state.get("interpretation", {}),
                        "observations": state.get("observations", []),
                        "planner_trace": state.get("planner_trace", []),
                    },
                    ensure_ascii=False,
                ),
            ]
        )

        projected_events = list(state.get("projected_events", []))
        streamed_chunks: list[str] = []
        for chunk in llm.stream_text(system_prompt=ANSWER_SYSTEM_PROMPT, user_prompt=stream_user_prompt):
            if not chunk:
                continue
            streamed_chunks.append(chunk)
            projected_events = append_projected_event(
                {**state, "projected_events": projected_events},
                "message.delta",
                {"delta": chunk},
            )

        answer_text = "".join(streamed_chunks).strip()
        if answer_text:
            metadata = llm.invoke_structured(
                schema=FinalAnswerMeta,
                system_prompt=METADATA_SYSTEM_PROMPT,
                user_prompt="\n".join(
                    [
                        "Return only the final answer metadata for this already-streamed answer.",
                        json.dumps(
                            {
                                "managed_context": state["managed_context"],
                                "interpretation": state.get("interpretation", {}),
                                "observations": state.get("observations", []),
                                "planner_trace": state.get("planner_trace", []),
                                "answer": answer_text,
                            },
                            ensure_ascii=False,
                        ),
                    ]
                ),
            )
            final_answer = {
                **metadata,
                "answer": answer_text,
            }
        else:
            final_answer = llm.invoke_structured(
                schema=FinalAnswer,
                system_prompt=FALLBACK_SYSTEM_PROMPT,
                user_prompt="\n".join(
                    [
                        "Return the full structured final answer.",
                        json.dumps(
                            {
                                "managed_context": state["managed_context"],
                                "interpretation": state.get("interpretation", {}),
                                "observations": state.get("observations", []),
                                "planner_trace": state.get("planner_trace", []),
                            },
                            ensure_ascii=False,
                        ),
                    ]
                ),
            )

        projected_events = append_projected_event(
            {**state, "projected_events": projected_events},
            "graph.node_completed",
            {"node": "responder_node", "graphName": "oj_tutor_supervisor"},
        )

        merged_state = dict(state)
        merged_state["final_answer_draft"] = final_answer
        artifact = project_artifact(merged_state)

        projected_events = append_projected_event(
            {**merged_state, "projected_events": projected_events},
            "artifact.created",
            {"artifactType": artifact.artifact_type, "artifactId": artifact.artifact_id},
            broadcast=False,
        )
        projected_events = append_projected_event(
            {**merged_state, "projected_events": projected_events},
            "run.completed",
            {"status": "SUCCEEDED", "activeNode": "responder_node"},
            broadcast=False,
        )

        return {
            "final_answer_draft": final_answer,
            "projected_artifacts": [artifact.model_dump(mode="json")],
            "projected_events": projected_events,
        }

    return node
