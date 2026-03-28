from typing import NotRequired, TypedDict


class StatusEventPayload(TypedDict):
    node: str
    message: str


class PracticeTopic(TypedDict):
    topic: str
    reason: str
    next_step: str


class QuestionRecommendation(TypedDict):
    question_id: str
    title: str
    difficulty: int | None
    source_query: str


class KnowledgeHit(TypedDict):
    source: str
    title: str
    excerpt: str
    score: float


class AgentState(TypedDict):
    trace_id: str
    user_id: str
    conversation_id: NotRequired[str | None]
    question_id: NotRequired[str | None]
    question_title: NotRequired[str | None]
    question_content: NotRequired[str | None]
    user_code: NotRequired[str | None]
    judge_result: NotRequired[str | None]
    user_message: str
    intent: NotRequired[str | None]
    context_gaps: NotRequired[list[str]]
    judge_signal: NotRequired[str | None]
    practice_topics: NotRequired[list[PracticeTopic]]
    diagnosis: NotRequired[dict[str, object]]
    explanation: NotRequired[dict[str, object]]
    practice_plan: NotRequired[dict[str, object]]
    question_recommendations: NotRequired[list[QuestionRecommendation]]
    knowledge_hits: NotRequired[list[KnowledgeHit]]
    memory_applied_fields: NotRequired[list[str]]
    previous_intent: NotRequired[str | None]
    previous_next_action: NotRequired[str | None]
    previous_answer: NotRequired[str | None]
    status_events: NotRequired[list[StatusEventPayload]]
    confidence: NotRequired[float]
    next_action: NotRequired[str]
    final_answer: NotRequired[str | None]
