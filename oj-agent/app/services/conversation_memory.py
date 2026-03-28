from collections.abc import Mapping
from copy import deepcopy
from threading import Lock
from typing import Any


REMEMBERED_CONTEXT_FIELDS = (
    "question_id",
    "question_title",
    "question_content",
    "user_code",
    "judge_result",
)

REMEMBERED_META_FIELDS = (
    "intent",
    "next_action",
    "final_answer",
)


class ConversationMemoryStore:
    def __init__(self) -> None:
        self._lock = Lock()
        self._store: dict[str, dict[str, Any]] = {}

    def recall(self, conversation_id: str | None) -> dict[str, Any]:
        if not conversation_id:
            return {}
        with self._lock:
            snapshot = self._store.get(conversation_id)
            if snapshot is None:
                return {}
            return deepcopy(snapshot)

    def remember(self, state: Mapping[str, Any]) -> None:
        conversation_id = state.get("conversation_id")
        if not conversation_id:
            return

        with self._lock:
            snapshot = dict(self._store.get(str(conversation_id), {}))
            for field in REMEMBERED_CONTEXT_FIELDS + REMEMBERED_META_FIELDS:
                value = state.get(field)
                if value is not None:
                    snapshot[field] = value
            self._store[str(conversation_id)] = snapshot

    def clear(self) -> None:
        with self._lock:
            self._store.clear()


conversation_memory_store = ConversationMemoryStore()
