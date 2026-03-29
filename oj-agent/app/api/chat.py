from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse

from app.runtime.engine import execute_chat_request
from app.runtime.streaming import stream_chat_request
from app.schemas.chat_request import ChatRequest
from app.schemas.chat_response import ChatResponse


router = APIRouter(prefix="/api/chat", tags=["chat"])


def _to_chat_response(state) -> ChatResponse:
    return ChatResponse(
        trace_id=state.request.trace_id,
        intent=state.outcome.intent or "ask_for_context",
        answer=state.outcome.answer or "I need more context before I can help precisely.",
        confidence=state.outcome.confidence or 0.0,
        next_action=state.outcome.next_action or "Send the question statement, current code, and latest judge result.",
    )


@router.post("", response_model=ChatResponse)
def chat(request: ChatRequest, raw_request: Request) -> ChatResponse:
    state = execute_chat_request(request, raw_request.headers)
    return _to_chat_response(state)


@router.post("/detail", response_model=ChatResponse)
def chat_detail(request: ChatRequest, raw_request: Request) -> ChatResponse:
    return chat(request, raw_request)


@router.post("/stream")
def chat_stream(request: ChatRequest, raw_request: Request) -> StreamingResponse:
    return StreamingResponse(
        stream_chat_request(request, raw_request.headers),
        media_type="text/event-stream",
    )
