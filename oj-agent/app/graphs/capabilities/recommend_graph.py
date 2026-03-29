from typing import TypedDict

from langgraph.graph import END, START, StateGraph


class RecommendGraphState(TypedDict):
    payload: dict


def recommend_node(state: RecommendGraphState) -> RecommendGraphState:
    return state


def build_recommend_graph():
    graph = StateGraph(RecommendGraphState)
    graph.add_node("recommend", recommend_node)
    graph.add_edge(START, "recommend")
    graph.add_edge("recommend", END)
    return graph.compile()
