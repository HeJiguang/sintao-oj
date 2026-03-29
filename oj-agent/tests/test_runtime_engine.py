from app.runtime.engine import execute_chat_request
from app.runtime.enums import RunStatus
from app.schemas.chat_request import ChatRequest


def test_execute_chat_request_returns_unified_agent_state():
    state = execute_chat_request(
        ChatRequest(
            trace_id="trace-runtime-001",
            user_id="u-1",
            conversation_id="conv-runtime-001",
            question_title="Two Sum",
            judge_result="WA on sample #2",
            user_code="public class Solution {}",
            user_message="Why is this WA?",
        ),
        {
            "X-User-Id": "u-1",
        },
    )

    assert state.execution.graph_name == "supervisor_graph"
    assert state.execution.status is RunStatus.SUCCEEDED
    assert state.outcome.intent == "analyze_failure"
    assert state.outcome.answer
    assert state.outcome.next_action
    assert state.outcome.status_events
