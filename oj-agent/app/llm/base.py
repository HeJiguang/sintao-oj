from abc import ABC, abstractmethod
from collections.abc import Iterator
from typing import Any


CHAT_CAPABILITY = "chat"
TRAINING_CAPABILITY = "training"


class LLMClient(ABC):

    @abstractmethod
    def is_available(self) -> bool:
        raise NotImplementedError

    @abstractmethod
    def model_name(self, capability: str) -> str:
        raise NotImplementedError

    @abstractmethod
    def generate_text(self, *, system_prompt: str, user_prompt: str, capability: str) -> str:
        raise NotImplementedError

    @abstractmethod
    def stream_text(self, *, system_prompt: str, user_prompt: str, capability: str) -> Iterator[str]:
        raise NotImplementedError

    @abstractmethod
    def generate_json(self, *, system_prompt: str, user_prompt: str, capability: str) -> dict[str, Any]:
        raise NotImplementedError
