import json

from app.runtime.contracts import PlannerAction
from app.runtime.live_events import append_projected_event


def build_planner_node(llm):
    """Choose the next tool call or terminate into the responder."""

    def node(state: dict) -> dict:
        planner_round = state.get("planner_round", 0) + 1
        planner_trace = list(state.get("planner_trace", []))
        action = llm.invoke_structured(
            schema=PlannerAction,
            system_prompt=(
                "You are the OnlineOJ planner node. "
                "Use managed_context, interpretation, observations, and planner_trace to choose the single next action."
            ),
            user_prompt="\n".join(
                [
                    "Return exactly one structured planner action.",
                    json.dumps(
                        {
                            "managed_context": state["managed_context"],
                            "interpretation": state.get("interpretation", {}),
                            "observations": state.get("observations", []),
                            "planner_round": planner_round,
                            "planner_trace": planner_trace,
                        },
                        ensure_ascii=False,
                    ),
                ]
            ),
        )
        planner_trace.append(action)

        projected_events = append_projected_event(
            state,
            "graph.node_completed",
            {
                "node": "planner_node",
                "graphName": "oj_tutor_supervisor",
                "plannerRound": planner_round,
                "actionType": action["type"],
                "tool": action.get("tool"),
                "reason": action.get("reason"),
            },
        )

        return {
            "planner_round": planner_round,
            "action": action,
            "planner_trace": planner_trace,
            "projected_events": projected_events,
        }

    return node
