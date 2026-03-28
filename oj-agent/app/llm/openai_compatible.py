from collections.abc import Iterator
import json
from typing import Any

from app.core.config import AgentSettings
from app.llm.base import CHAT_CAPABILITY, LLMClient, TRAINING_CAPABILITY


class OpenAICompatibleLLMClient(LLMClient):
    def __init__(self, settings: AgentSettings) -> None:
        self.settings = settings
        self._client = None

    def is_available(self) -> bool:
        return bool(self.settings.llm_api_key and (self.settings.chat_model or self.settings.training_model))

    def model_name(self, capability: str) -> str:
        if capability == TRAINING_CAPABILITY and self.settings.training_model:
            return self.settings.training_model
        if capability == CHAT_CAPABILITY and self.settings.chat_model:
            return self.settings.chat_model
        return self.settings.chat_model or self.settings.training_model or "unknown-model"

    def generate_text(self, *, system_prompt: str, user_prompt: str, capability: str) -> str:
        message = self._client_instance().chat.completions.create(
            model=self.model_name(capability),
            temperature=self.settings.llm_temperature,
            max_tokens=self.settings.llm_max_tokens,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        )
        content = message.choices[0].message.content
        return content if isinstance(content, str) else str(content or "")

    def stream_text(self, *, system_prompt: str, user_prompt: str, capability: str) -> Iterator[str]:
        stream = self._client_instance().chat.completions.create(
            model=self.model_name(capability),
            temperature=self.settings.llm_temperature,
            max_tokens=self.settings.llm_max_tokens,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            stream=True,
        )
        for chunk in stream:
            if not chunk.choices:
                continue
            delta = chunk.choices[0].delta.content
            if isinstance(delta, str) and delta:
                yield delta

    def generate_json(self, *, system_prompt: str, user_prompt: str, capability: str) -> dict[str, Any]:
        text = self.generate_text(
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            capability=capability,
        )
        return _extract_json_object(text)

    def _client_instance(self):
        if self._client is None:
            try:
                from openai import OpenAI
            except ImportError as exc:
                raise RuntimeError("openai package is not installed") from exc

            self._client = OpenAI(
                api_key=self.settings.llm_api_key,
                base_url=self.settings.llm_base_url,
                timeout=self.settings.llm_timeout_seconds,
            )
        return self._client


def _extract_json_object(text: str) -> dict[str, Any]:
    stripped = text.strip()
    start = stripped.find("{")
    end = stripped.rfind("}")
    if start < 0 or end < 0 or end <= start:
        raise ValueError("model response did not contain a JSON object")
    return json.loads(stripped[start : end + 1])
