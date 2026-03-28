from datetime import datetime

from pydantic import BaseModel, Field


class SubmissionSnapshot(BaseModel):
    submit_id: int | None = Field(default=None, description="Submit id")
    question_id: int | None = Field(default=None, description="Question id")
    exam_id: int | None = Field(default=None, description="Test id")
    title: str | None = Field(default=None, description="Question title")
    difficulty: int | None = Field(default=None, description="Question difficulty")
    algorithm_tag: str | None = Field(default=None, description="Primary algorithm tag")
    knowledge_tags: str | None = Field(default=None, description="Comma separated knowledge tags")
    pass_: int | None = Field(default=None, alias="pass", description="Judge pass flag")
    score: int | None = Field(default=None, description="Submit score")
    exe_message: str | None = Field(default=None, description="Judge execution message")

    model_config = {"populate_by_name": True}


class QuestionCandidate(BaseModel):
    question_id: int = Field(..., description="Question id")
    title: str = Field(..., description="Question title")
    difficulty: int | None = Field(default=None, description="Question difficulty")
    algorithm_tag: str | None = Field(default=None, description="Primary algorithm tag")
    knowledge_tags: str | None = Field(default=None, description="Comma separated knowledge tags")
    estimated_minutes: int | None = Field(default=None, description="Estimated solving time")


class ExamCandidate(BaseModel):
    exam_id: int = Field(..., description="Stage test id")
    title: str = Field(..., description="Stage test title")
    start_time: datetime | None = Field(default=None, description="Stage test start time")
    end_time: datetime | None = Field(default=None, description="Stage test end time")


class TrainingPlanRequest(BaseModel):
    trace_id: str = Field(..., description="Trace id")
    user_id: int = Field(..., description="User id")
    current_level: str | None = Field(default=None, description="Current level")
    target_direction: str | None = Field(default=None, description="Target direction")
    based_on_exam_id: int | None = Field(default=None, description="Optional source test id")
    preferred_count: int | None = Field(default=None, description="Preferred task count")
    recent_submissions: list[SubmissionSnapshot] = Field(default_factory=list, description="Recent submission summaries")
    candidate_questions: list[QuestionCandidate] = Field(default_factory=list, description="Candidate questions")
    candidate_exams: list[ExamCandidate] = Field(default_factory=list, description="Candidate stage tests")
