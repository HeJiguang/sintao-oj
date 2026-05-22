from typing import TypedDict

from langgraph.graph import END, START, StateGraph

from app.runtime.nodes import (
    build_context_node,
    build_planner_node,
    build_responder_node,
    build_tool_node,
)
from app.runtime.tools import ToolRegistry


class RuntimeState(TypedDict, total=False):
    """LangGraph 在运行过程中共享的显式状态对象。"""

    run_id: str
    run_type: str
    status: str
    managed_context: dict
    interpretation: dict
    action: dict
    observations: list[dict]
    planner_round: int
    planner_trace: list[dict]
    final_answer_draft: dict
    projected_events: list[dict]
    projected_artifacts: list[dict]
    errors: list[str]
    event_sink: object


def build_runtime_graph(*, llm, tool_registry: ToolRegistry):
    """构建当前 agent 的四节点 LangGraph 主图。"""
    graph = StateGraph(RuntimeState)

    graph.add_node("context_node", build_context_node(llm))
    graph.add_node("planner_node", build_planner_node(llm))
    graph.add_node("tool_node", build_tool_node(tool_registry))
    graph.add_node("responder_node", build_responder_node(llm))

    graph.add_edge(START, "context_node")
    graph.add_edge("context_node", "planner_node")
    graph.add_conditional_edges(
        "planner_node",
        _route_after_planner,
        {
            "tool_node": "tool_node",
            "responder_node": "responder_node",
        },
    )
    graph.add_edge("tool_node", "planner_node")
    graph.add_edge("responder_node", END)
    return graph.compile()


def _route_after_planner(state: RuntimeState) -> str:
    """根据 planner 给出的动作类型，决定继续调工具还是直接收束回答。"""
    if state["action"]["type"] == "tool":
        return "tool_node"
    return "responder_node"
