from collections import Counter
from datetime import datetime
import json

from app.llm.base import TRAINING_CAPABILITY
from app.llm.factory import build_llm_client
from app.schemas.training_plan_request import (
    ExamCandidate,
    QuestionCandidate,
    SubmissionSnapshot,
    TrainingPlanRequest,
)
from app.schemas.training_plan_response import TrainingPlanResponse, TrainingPlanTask


def _split_tags(raw: str | None) -> list[str]:
    if not raw:
        return []
    return [part.strip() for part in raw.split(",") if part.strip()]


def _infer_level(request: TrainingPlanRequest) -> str:
    if request.current_level:
        return request.current_level

    if not request.recent_submissions:
        return "starter"

    passed = 0
    scored = 0
    counted = 0
    for submission in request.recent_submissions:
        if submission.pass_ == 1:
            passed += 1
        if submission.difficulty is not None:
            scored += submission.difficulty
            counted += 1

    pass_ratio = passed / len(request.recent_submissions)
    avg_difficulty = scored / counted if counted else 1.0

    if pass_ratio >= 0.7 and avg_difficulty >= 3:
        return "advanced"
    if pass_ratio >= 0.45 or avg_difficulty >= 2:
        return "intermediate"
    return "starter"


def _focus_counter(submissions: list[SubmissionSnapshot]) -> Counter[str]:
    counter: Counter[str] = Counter()
    for submission in submissions:
        if submission.pass_ == 1:
            continue
        for tag in _split_tags(submission.algorithm_tag):
            counter[tag] += 2
        for tag in _split_tags(submission.knowledge_tags):
            counter[tag] += 1
    return counter


def _strength_counter(submissions: list[SubmissionSnapshot]) -> Counter[str]:
    counter: Counter[str] = Counter()
    for submission in submissions:
        if submission.pass_ != 1:
            continue
        for tag in _split_tags(submission.algorithm_tag):
            counter[tag] += 2
        for tag in _split_tags(submission.knowledge_tags):
            counter[tag] += 1
    return counter


def _level_target_difficulty(level: str) -> int:
    if level == "advanced":
        return 3
    if level == "intermediate":
        return 2
    return 1


def _candidate_score(candidate: QuestionCandidate, focus_tags: list[str], level: str, seen_question_ids: set[int]) -> int:
    score = 0
    if candidate.question_id in seen_question_ids:
        score -= 40

    candidate_tags = set(_split_tags(candidate.algorithm_tag) + _split_tags(candidate.knowledge_tags))
    if candidate_tags.intersection(focus_tags):
        score += 120

    target_difficulty = _level_target_difficulty(level)
    difficulty = candidate.difficulty or 1
    score -= abs(difficulty - target_difficulty) * 10

    if candidate.estimated_minutes is not None:
        score -= max(candidate.estimated_minutes - 20, 0) // 5
    return score


def _pick_stage_test(request: TrainingPlanRequest) -> ExamCandidate | None:
    if not request.candidate_exams and request.based_on_exam_id is None:
        return None

    by_id = {candidate.exam_id: candidate for candidate in request.candidate_exams}
    if request.based_on_exam_id is not None:
        matched = by_id.get(request.based_on_exam_id)
        if matched is not None:
            return matched
        return ExamCandidate(
            exam_id=request.based_on_exam_id,
            title="Stage test recheck",
            start_time=None,
            end_time=None,
        )

    now = datetime.now()
    active = [
        candidate
        for candidate in request.candidate_exams
        if candidate.start_time is not None
        and candidate.end_time is not None
        and candidate.start_time <= now <= candidate.end_time
    ]
    if active:
        return sorted(active, key=lambda candidate: (candidate.end_time or now, candidate.title))[0]

    upcoming = [
        candidate
        for candidate in request.candidate_exams
        if candidate.end_time is None or candidate.end_time > now
    ]
    if upcoming:
        return sorted(
            upcoming,
            key=lambda candidate: (
                candidate.start_time is None,
                candidate.start_time or now,
                candidate.title,
            ),
        )[0]

    return None


def _build_heuristic_training_plan(request: TrainingPlanRequest) -> TrainingPlanResponse:
    level = _infer_level(request)
    target_direction = request.target_direction or "algorithm_foundation"

    weak_counter = _focus_counter(request.recent_submissions)
    strong_counter = _strength_counter(request.recent_submissions)
    focus_tags = [tag for tag, _ in weak_counter.most_common(2)]
    strong_tags = [tag for tag, _ in strong_counter.most_common(2)]

    if not focus_tags:
        focus_tags = ["problem solving rhythm"]
    if not strong_tags:
        strong_tags = ["consistent practice"]

    weak_points = ", ".join(focus_tags)
    strong_points = ", ".join(strong_tags)

    preferred_count = request.preferred_count or 5
    seen_question_ids = {submission.question_id for submission in request.recent_submissions if submission.question_id is not None}

    ranked_candidates = sorted(
        request.candidate_questions,
        key=lambda candidate: (
            -_candidate_score(candidate, focus_tags, level, seen_question_ids),
            candidate.difficulty or 1,
            candidate.title,
        ),
    )

    tasks: list[TrainingPlanTask] = []
    task_order = 1

    failed_submissions = [submission for submission in request.recent_submissions if submission.pass_ == 0 and submission.question_id is not None]
    if failed_submissions:
        review_source = failed_submissions[0]
        tasks.append(
            TrainingPlanTask(
                task_type="review",
                question_id=review_source.question_id,
                exam_id=review_source.exam_id,
                title_snapshot=review_source.title or "Review your recent failed question",
                task_order=task_order,
                recommended_reason="Start by revisiting a recent failed submission and understanding the mistake pattern.",
                knowledge_tags_snapshot=review_source.knowledge_tags or review_source.algorithm_tag,
            )
        )
        task_order += 1

    picked_ids: set[int] = set()
    for candidate in ranked_candidates:
        if len(tasks) >= preferred_count:
            break
        if candidate.question_id in picked_ids:
            continue
        picked_ids.add(candidate.question_id)
        tasks.append(
            TrainingPlanTask(
                task_type="question",
                question_id=candidate.question_id,
                exam_id=None,
                title_snapshot=candidate.title,
                task_order=task_order,
                recommended_reason=f"Matches current focus: {weak_points}.",
                knowledge_tags_snapshot=candidate.knowledge_tags or candidate.algorithm_tag,
            )
        )
        task_order += 1

    stage_test = _pick_stage_test(request)
    if stage_test is not None and len(tasks) < preferred_count + 1:
        tasks.append(
            TrainingPlanTask(
                task_type="test",
                question_id=None,
                exam_id=stage_test.exam_id,
                title_snapshot=stage_test.title,
                task_order=task_order,
                recommended_reason="Use the stage test checkpoint to verify whether this round of practice improved stability.",
                knowledge_tags_snapshot=weak_points,
                due_time=stage_test.end_time or stage_test.start_time,
            )
        )

    plan_title = f"{target_direction.replace('_', ' ').title()} Training Plan"
    plan_goal = f"Focus on {weak_points} with {len(tasks)} ordered tasks."
    ai_summary = (
        f"Current level is {level}. "
        f"Main weak points: {weak_points}. "
        f"Main strengths: {strong_points}. "
        f"Use this plan to build a steadier solving rhythm."
    )

    return TrainingPlanResponse(
        current_level=level,
        target_direction=target_direction,
        weak_points=weak_points,
        strong_points=strong_points,
        plan_title=plan_title,
        plan_goal=plan_goal,
        ai_summary=ai_summary,
        tasks=tasks,
    )


def _generate_plan_with_llm(request: TrainingPlanRequest, heuristic_plan: TrainingPlanResponse) -> dict:
    llm_client = build_llm_client()
    if not llm_client.is_available():
        raise RuntimeError("llm planner is unavailable")

    system_prompt = "\n".join(
        [
            "You are SynCode's training-plan generator for an online judge learning platform.",
            "Generate only structured JSON.",
            "Choose tasks only from the provided candidate question ids and candidate exam ids.",
            "Keep the response compact and execution-oriented.",
        ]
    )
    user_prompt = "\n".join(
        [
            "Return a JSON object with these fields:",
            "current_level, target_direction, weak_points, strong_points, plan_title, plan_goal, ai_summary, tasks.",
            "Each task must include: task_type, question_id, exam_id, title_snapshot, task_order, recommended_reason, knowledge_tags_snapshot.",
            "Allowed task_type values: review, question, test.",
            "",
            "Heuristic fallback draft:",
            json.dumps(heuristic_plan.model_dump(mode='json'), ensure_ascii=False),
            "",
            "Current request:",
            json.dumps(request.model_dump(mode='json', by_alias=True), ensure_ascii=False),
        ]
    )
    return llm_client.generate_json(
        system_prompt=system_prompt,
        user_prompt=user_prompt,
        capability=TRAINING_CAPABILITY,
    )


def _normalize_llm_plan(
    request: TrainingPlanRequest,
    heuristic_plan: TrainingPlanResponse,
    llm_payload: dict,
) -> TrainingPlanResponse:
    candidate_question_ids = {candidate.question_id for candidate in request.candidate_questions}
    candidate_exam_ids = {candidate.exam_id for candidate in request.candidate_exams}
    if request.based_on_exam_id is not None:
        candidate_exam_ids.add(request.based_on_exam_id)

    normalized_tasks: list[dict] = []
    for index, raw_task in enumerate(llm_payload.get("tasks", []), start=1):
        task_type = raw_task.get("task_type") or "question"
        question_id = raw_task.get("question_id")
        exam_id = raw_task.get("exam_id")
        if task_type == "test":
            if exam_id not in candidate_exam_ids:
                continue
            question_id = None
        else:
            if question_id not in candidate_question_ids:
                continue
            exam_id = None

        normalized_tasks.append(
            {
                "task_type": task_type,
                "question_id": question_id,
                "exam_id": exam_id,
                "title_snapshot": raw_task.get("title_snapshot") or f"Training task {index}",
                "task_order": raw_task.get("task_order") or index,
                "recommended_reason": raw_task.get("recommended_reason"),
                "knowledge_tags_snapshot": raw_task.get("knowledge_tags_snapshot"),
                "due_time": raw_task.get("due_time"),
            }
        )

    if not normalized_tasks:
        raise ValueError("llm plan did not produce any valid tasks")

    merged_payload = {
        "current_level": llm_payload.get("current_level") or heuristic_plan.current_level,
        "target_direction": llm_payload.get("target_direction") or heuristic_plan.target_direction,
        "weak_points": llm_payload.get("weak_points") or heuristic_plan.weak_points,
        "strong_points": llm_payload.get("strong_points") or heuristic_plan.strong_points,
        "plan_title": llm_payload.get("plan_title") or heuristic_plan.plan_title,
        "plan_goal": llm_payload.get("plan_goal") or heuristic_plan.plan_goal,
        "ai_summary": llm_payload.get("ai_summary") or heuristic_plan.ai_summary,
        "tasks": normalized_tasks,
    }
    return TrainingPlanResponse.model_validate(merged_payload)


def build_training_plan(request: TrainingPlanRequest) -> TrainingPlanResponse:
    heuristic_plan = _build_heuristic_training_plan(request)
    try:
        llm_payload = _generate_plan_with_llm(request, heuristic_plan)
        return _normalize_llm_plan(request, heuristic_plan, llm_payload)
    except Exception:
        return heuristic_plan
