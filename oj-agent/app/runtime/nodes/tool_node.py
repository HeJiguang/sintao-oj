from app.runtime.live_events import append_projected_event


def build_tool_node(tool_registry):
    """Execute the tool selected by the planner and record its observation."""

    def node(state: dict) -> dict:
        observations = list(state.get("observations", []))
        observation = tool_registry.invoke(
            state["action"]["tool"],
            state["managed_context"],
            state["action"].get("input", {}),
        )
        observations.append(observation)

        projected_events = append_projected_event(
            state,
            "tool.completed" if observation.get("ok", True) else "tool.failed",
            {
                "node": "tool_node",
                "tool": observation["tool_name"],
                "ok": observation.get("ok", True),
                "error": observation.get("error"),
            },
        )

        if observation.get("ok", True) and observation["tool_name"] == "retrieve_knowledge_evidence":
            items = observation.get("payload", {}).get("items", [])
            projected_events = append_projected_event(
                {**state, "projected_events": projected_events},
                "retrieval.evidence_ready",
                {
                    "graphName": "oj_tutor_supervisor",
                    "evidenceCount": len(items),
                    "sources": [item.get("source_id") for item in items if item.get("source_id")],
                },
            )

        return {
            "observations": observations,
            "projected_events": projected_events,
        }

    return node
