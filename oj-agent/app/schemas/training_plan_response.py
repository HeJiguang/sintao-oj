from datetime import datetime

from pydantic import BaseModel, Field


class TrainingPlanTask(BaseModel):
    task_type: str = Field(..., description="Task type such as question or test")
    question_id: int | None = Field(default=None, description="Question id when task type is question")
    exam_id: int | None = Field(default=None, description="Test id when task type is test")
    title_snapshot: str = Field(..., description="Task title")
    task_order: int = Field(..., description="Ordered position in the plan")
    recommended_reason: str | None = Field(default=None, description="Why this task was recommended")
    knowledge_tags_snapshot: str | None = Field(default=None, description="Knowledge tags snapshot")
    due_time: datetime | None = Field(default=None, description="Recommended completion time for the task")


class TrainingPlanResponse(BaseModel):
    current_level: str = Field(..., description="Resolved current level")
    target_direction: str = Field(..., description="Resolved target direction")
    weak_points: str = Field(..., description="Weak points summary")
    strong_points: str = Field(..., description="Strong points summary")
    plan_title: str = Field(..., description="Plan title")
    plan_goal: str = Field(..., description="Plan goal")
    ai_summary: str = Field(..., description="Plan summary")
    tasks: list[TrainingPlanTask] = Field(default_factory=list, description="Generated tasks")
