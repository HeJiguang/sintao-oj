from collections.abc import Iterator
from typing import Any

from app.llm.base import LLMClient


class UnavailableLLMClient(LLMClient):
    def __init__(self, reason: str = "llm unavailable") -> None:
        self._reason = reason

    def is_available(self) -> bool:
        return False

    def model_name(self, capability: str) -> str:
        return "heuristic-fallback"

    def generate_text(self, *, system_prompt: str, user_prompt: str, capability: str) -> str:
        raise RuntimeError(self._reason)

    def stream_text(self, *, system_prompt: str, user_prompt: str, capability: str) -> Iterator[str]:
        raise RuntimeError(self._reason)

    def generate_json(self, *, system_prompt: str, user_prompt: str, capability: str) -> dict[str, Any]:
        raise RuntimeError(self._reason)
