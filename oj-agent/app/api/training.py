from datetime import datetime, timedelta, timezone
from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel, Field


router = APIRouter(prefix="/api/training", tags=["training"])


class SubmissionSnapshot(BaseModel):
    submit_id: int | None = None
    question_id: int | None = None
    exam_id: int | None = None
    title: str | None = None
    difficulty: int | None = None
    algorithm_tag: str | None = None
    knowledge_tags: str | None = None
    pass_: int | None = Field(default=None, alias="pass")
    score: int | None = None
    exe_message: str | None = None


class QuestionCandidate(BaseModel):
    question_id: int | None = None
    title: str | None = None
    difficulty: int | None = None
    algorithm_tag: str | None = None
    knowledge_tags: str | None = None
    estimated_minutes: int | None = None


class ExamCandidate(BaseModel):
    exam_id: int | None = None
    title: str | None = None
    start_time: str | None = None
    end_time: str | None = None


class TrainingPlanRequest(BaseModel):
    trace_id: str | None = None
    user_id: int | None = None
    current_level: str | None = None
    target_direction: str | None = None
    based_on_exam_id: int | None = None
    preferred_count: int | None = None
    recent_submissions: list[SubmissionSnapshot] = Field(default_factory=list)
    candidate_questions: list[QuestionCandidate] = Field(default_factory=list)
    candidate_exams: list[ExamCandidate] = Field(default_factory=list)


def _due_time(days_from_now: int) -> str:
    return (datetime.now(timezone.utc) + timedelta(days=days_from_now)).replace(tzinfo=None, microsecond=0).isoformat()


def _build_question_task(question: QuestionCandidate, task_order: int) -> dict[str, Any]:
    title = question.title or f"Training question {task_order}"
    tags = question.knowledge_tags or question.algorithm_tag or "algorithm_foundation"
    return {
        "task_type": "question",
        "question_id": question.question_id,
        "title_snapshot": title,
        "task_order": task_order,
        "recommended_reason": f"Warm up with {title} to stabilize the current weak point.",
        "knowledge_tags_snapshot": tags,
        "due_time": _due_time(task_order),
    }


def _build_exam_task(exam: ExamCandidate, task_order: int) -> dict[str, Any]:
    title = exam.title or "Stage test checkpoint"
    return {
        "task_type": "test",
        "exam_id": exam.exam_id,
        "title_snapshot": title,
        "task_order": task_order,
        "recommended_reason": f"Use {title} to verify whether the refreshed practice plan is effective.",
        "knowledge_tags_snapshot": "timed_assessment",
        "due_time": exam.end_time or _due_time(task_order + 1),
    }


@router.post("/plan")
def create_training_plan(request: TrainingPlanRequest) -> dict[str, Any]:
    preferred_count = request.preferred_count if request.preferred_count and request.preferred_count > 0 else 5
    reserve_exam_slot = 1 if request.candidate_exams and preferred_count > 1 else 0
    selected_questions = request.candidate_questions[: max(1, preferred_count - reserve_exam_slot)]
    tasks = [_build_question_task(question, index + 1) for index, question in enumerate(selected_questions)]

    if request.candidate_exams:
        tasks.append(_build_exam_task(request.candidate_exams[0], len(tasks) + 1))

    if not tasks:
        tasks.append(
            {
                "task_type": "question",
                "question_id": None,
                "title_snapshot": "Rebuild the current practice rhythm",
                "task_order": 1,
                "recommended_reason": "No candidate content was provided, so start with a lightweight manual refresh task.",
                "knowledge_tags_snapshot": request.target_direction or "algorithm_foundation",
                "due_time": _due_time(1),
            }
        )

    weak_points = "Need more repetition on failed or unstable topics."
    if request.recent_submissions:
        latest = request.recent_submissions[0]
        weak_points = latest.exe_message or weak_points

    return {
        "current_level": request.current_level or "starter",
        "target_direction": request.target_direction or "algorithm_foundation",
        "weak_points": weak_points,
        "strong_points": "Has enough recent activity to generate a focused plan.",
        "plan_title": "AI personalized training plan",
        "plan_goal": "Build a stable algorithm practice routine",
        "ai_summary": "Route the learner through focused questions first, then validate with a checkpoint exam.",
        "tasks": tasks,
    }
