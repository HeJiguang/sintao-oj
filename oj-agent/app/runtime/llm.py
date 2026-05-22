from collections.abc import Iterator
from typing import Any

from langchain_openai import ChatOpenAI

from app.config import AgentSettings, load_settings


class DeepSeekLLM:
    """Thin wrapper over the real model client used by the runtime nodes."""

    def __init__(self, settings: AgentSettings) -> None:
        self.settings = settings
        self.model = ChatOpenAI(
            model=settings.llm_model,
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url,
            temperature=settings.llm_temperature,
            max_tokens=settings.llm_max_tokens,
        )

    def invoke_structured(self, *, schema: Any, system_prompt: str, user_prompt: str) -> dict:
        structured_llm = self.model.with_structured_output(schema, method="function_calling")
        result = structured_llm.invoke(
            [
                ("system", system_prompt),
                ("user", user_prompt),
            ]
        )
        return result.model_dump(mode="json")

    def stream_text(self, *, system_prompt: str, user_prompt: str) -> Iterator[str]:
        for chunk in self.model.stream(
            [
                ("system", system_prompt),
                ("user", user_prompt),
            ]
        ):
            text = _coerce_chunk_text(getattr(chunk, "content", ""))
            if text:
                yield text


def _coerce_chunk_text(content: Any) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, str):
                parts.append(item)
                continue
            if not isinstance(item, dict):
                continue
            if isinstance(item.get("text"), str):
                parts.append(item["text"])
        return "".join(parts)
    return ""


def build_default_llm(settings: AgentSettings | None = None) -> DeepSeekLLM:
    resolved = settings or load_settings()
    if not resolved.real_llm_enabled:
        raise RuntimeError(
            "Real LLM configuration is required. Set OJ_AGENT_LLM_PROVIDER=deepseek and a valid OJ_AGENT_LLM_API_KEY."
        )
    return DeepSeekLLM(resolved)
