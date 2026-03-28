from typing import Any

from pydantic import BaseModel, ConfigDict, Field, model_validator


class ChatRequest(BaseModel):
    trace_id: str | None = Field(default=None, description="Request trace identifier")
    user_id: str | None = Field(default=None, description="Current user identifier")
    conversation_id: str | None = Field(default=None, description="Conversation id")
    question_id: str | None = Field(default=None, description="Current OJ question id")
    question_title: str | None = Field(default=None, description="Current OJ question title")
    question_content: str | None = Field(default=None, description="Current OJ problem statement")
    user_code: str | None = Field(default=None, description="User code under discussion")
    judge_result: str | None = Field(default=None, description="Latest judge result summary")
    user_message: str = Field(..., description="Latest user message")

    model_config = ConfigDict(populate_by_name=True)

    @model_validator(mode="before")
    @classmethod
    def normalize_camel_case(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data
        normalized = dict(data)
        key_map = {
            "traceId": "trace_id",
            "userId": "user_id",
            "conversationId": "conversation_id",
            "questionId": "question_id",
            "questionTitle": "question_title",
            "questionContent": "question_content",
            "userCode": "user_code",
            "judgeResult": "judge_result",
            "userMessage": "user_message",
        }
        for source_key, target_key in key_map.items():
            if source_key in normalized and target_key not in normalized:
                normalized[target_key] = normalized[source_key]
        return normalized
