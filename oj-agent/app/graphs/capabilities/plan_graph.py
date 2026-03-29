from typing import NotRequired, TypedDict

from langgraph.graph import END, START, StateGraph

from app.graphs.capabilities.plan_nodes import (
    candidate_retrieval_node,
    draft_planning_node,
    gap_analysis_node,
    intake_node,
    packaging_node,
    repair_node,
    verification_node,
)
from app.schemas.training_plan_request import TrainingPlanRequest
from app.schemas.training_plan_response import TrainingPlanResponse


class PlanGraphState(TypedDict):
    request: TrainingPlanRequest
    inferred_level: NotRequired[str]
    focus_tags: NotRequired[list[str]]
    candidate_question_count: NotRequired[int]
    candidate_exam_count: NotRequired[int]
    stage_test_exam_id: NotRequired[int | None]
    heuristic_plan: NotRequired[TrainingPlanResponse]
    llm_payload: NotRequired[dict | None]
    verification_errors: NotRequired[list[str]]
    response: NotRequired[TrainingPlanResponse]


def route_after_verification(state: PlanGraphState) -> str:
    if state.get("verification_errors"):
        return "repair"
    return "packaging"


def build_plan_graph():
    graph = StateGraph(PlanGraphState)
    graph.add_node("intake", intake_node)
    graph.add_node("gap_analysis", gap_analysis_node)
    graph.add_node("candidate_retrieval", candidate_retrieval_node)
    graph.add_node("draft_planning", draft_planning_node)
    graph.add_node("verification", verification_node)
    graph.add_node("repair", repair_node)
    graph.add_node("packaging", packaging_node)

    graph.add_edge(START, "intake")
    graph.add_edge("intake", "gap_analysis")
    graph.add_edge("gap_analysis", "candidate_retrieval")
    graph.add_edge("candidate_retrieval", "draft_planning")
    graph.add_edge("draft_planning", "verification")
    graph.add_conditional_edges(
        "verification",
        route_after_verification,
        {
            "repair": "repair",
            "packaging": "packaging",
        },
    )
    graph.add_edge("repair", "packaging")
    graph.add_edge("packaging", END)
    return graph.compile()
