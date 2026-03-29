from app.services.training_planner import _pick_stage_test


def candidate_retrieval_node(state: dict) -> dict:
    request = state["request"]
    stage_test = _pick_stage_test(request)
    return {
        **state,
        "candidate_question_count": len(request.candidate_questions),
        "candidate_exam_count": len(request.candidate_exams),
        "stage_test_exam_id": stage_test.exam_id if stage_test is not None else None,
    }
