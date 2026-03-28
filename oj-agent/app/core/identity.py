from collections.abc import Mapping
from uuid import uuid4

from fastapi import HTTPException, status

from app.schemas.chat_request import ChatRequest


def normalize_chat_request(request: ChatRequest, headers: Mapping[str, str | None], user_id_header: str) -> ChatRequest:
    resolved_user_id = request.user_id or headers.get(user_id_header)
    if not resolved_user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing authenticated user id")

    resolved_trace_id = request.trace_id or str(uuid4())
    return request.model_copy(
        update={
            "user_id": str(resolved_user_id),
            "trace_id": resolved_trace_id,
        }
    )
