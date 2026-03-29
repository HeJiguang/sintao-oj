from typing import TypedDict

from langgraph.graph import END, START, StateGraph


class ProfileGraphState(TypedDict):
    payload: dict


def profile_node(state: ProfileGraphState) -> ProfileGraphState:
    return state


def build_profile_graph():
    graph = StateGraph(ProfileGraphState)
    graph.add_node("profile", profile_node)
    graph.add_edge(START, "profile")
    graph.add_edge("profile", END)
    return graph.compile()
