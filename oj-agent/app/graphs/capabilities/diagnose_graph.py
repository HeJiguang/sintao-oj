from typing import TypedDict

from langgraph.graph import END, START, StateGraph


class DiagnoseGraphState(TypedDict):
    payload: dict


def diagnose_node(state: DiagnoseGraphState) -> DiagnoseGraphState:
    return state


def build_diagnose_graph():
    graph = StateGraph(DiagnoseGraphState)
    graph.add_node("diagnose", diagnose_node)
    graph.add_edge(START, "diagnose")
    graph.add_edge("diagnose", END)
    return graph.compile()
