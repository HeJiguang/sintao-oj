from langgraph.graph import END, START, StateGraph

from app.graph.edges import ANALYZE_FAILURE, ASK_FOR_CONTEXT, EXPLAIN_PROBLEM, RECOMMEND_QUESTION
from app.graph.nodes.context_analyzer import context_analyzer_node
from app.graph.nodes.failure_diagnoser import failure_diagnoser_node
from app.graph.nodes.finalizer import finalizer_node
from app.graph.nodes.ingress import ingress_node
from app.graph.nodes.knowledge_retriever import knowledge_retriever_node
from app.graph.nodes.practice_recommender import practice_recommender_node
from app.graph.nodes.problem_explainer import problem_explainer_node
from app.graph.nodes.router import router_node
from app.graph.state import AgentState


def route_after_router(state: AgentState) -> str:
    return state.get("intent") or ASK_FOR_CONTEXT


def build_graph():
    graph = StateGraph(AgentState)
    graph.add_node("ingress", ingress_node)
    graph.add_node("context_analyzer", context_analyzer_node)
    graph.add_node("knowledge_retriever", knowledge_retriever_node)
    graph.add_node("router", router_node)
    graph.add_node("failure_diagnoser", failure_diagnoser_node)
    graph.add_node("problem_explainer", problem_explainer_node)
    graph.add_node("practice_recommender", practice_recommender_node)
    graph.add_node("finalizer", finalizer_node)

    graph.add_edge(START, "ingress")
    graph.add_edge("ingress", "context_analyzer")
    graph.add_edge("context_analyzer", "knowledge_retriever")
    graph.add_edge("knowledge_retriever", "router")
    graph.add_conditional_edges(
        "router",
        route_after_router,
        {
            ANALYZE_FAILURE: "failure_diagnoser",
            EXPLAIN_PROBLEM: "problem_explainer",
            RECOMMEND_QUESTION: "practice_recommender",
            ASK_FOR_CONTEXT: "finalizer",
        },
    )
    graph.add_edge("failure_diagnoser", "finalizer")
    graph.add_edge("problem_explainer", "finalizer")
    graph.add_edge("practice_recommender", "finalizer")
    graph.add_edge("finalizer", END)

    return graph.compile()
