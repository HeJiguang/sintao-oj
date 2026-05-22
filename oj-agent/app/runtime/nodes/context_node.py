import json

from app.runtime.contracts import ContextEnvelope
from app.runtime.live_events import append_projected_event


def build_context_node(llm):
    """Normalize the shared managed context before planning begins."""

    def node(state: dict) -> dict:
        projected_events = append_projected_event(
            state,
            "run.started",
            {"activeNode": "context_node"},
        )

        interpretation = llm.invoke_structured(
            schema=ContextEnvelope,
            system_prompt=(
                "You are the OnlineOJ context node. "
                "Understand the managed_context, infer the user's goal, and identify missing context for later nodes."
            ),
            user_prompt="\n".join(
                [
                    "Return a structured interpretation for the following managed_context.",
                    json.dumps({"managed_context": state["managed_context"]}, ensure_ascii=False),
                ]
            ),
        )

        projected_events = append_projected_event(
            {**state, "projected_events": projected_events},
            "graph.node_completed",
            {"node": "context_node", "graphName": "oj_tutor_supervisor"},
        )

        return {
            "status": "RUNNING",
            "interpretation": interpretation,
            "projected_events": projected_events,
        }

    return node
