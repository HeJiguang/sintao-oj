from app.runtime.enums import TaskType
from app.runtime.models import RequestContext


def build_request_context(
    *,
    trace_id: str,
    user_id: str,
    task_type: TaskType,
    user_message: str,
    conversation_id: str | None = None,
    question_id: str | None = None,
    question_title: str | None = None,
    question_content: str | None = None,
    user_code: str | None = None,
    judge_result: str | None = None,
    exam_id: str | None = None,
    plan_id: str | None = None,
) -> RequestContext:
    return RequestContext(
        trace_id=trace_id,
        user_id=user_id,
        task_type=task_type,
        user_message=user_message,
        conversation_id=conversation_id,
        question_id=question_id,
        question_title=question_title,
        question_content=question_content,
        user_code=user_code,
        judge_result=judge_result,
        exam_id=exam_id,
        plan_id=plan_id,
    )
