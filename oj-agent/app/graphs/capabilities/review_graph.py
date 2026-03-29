from typing import TypedDict

from langgraph.graph import END, START, StateGraph


class ReviewGraphState(TypedDict):
    payload: dict


def review_node(state: ReviewGraphState) -> ReviewGraphState:
    return state


def build_review_graph():
    graph = StateGraph(ReviewGraphState)
    graph.add_node("review", review_node)
    graph.add_edge(START, "review")
    graph.add_edge("review", END)
    return graph.compile()
