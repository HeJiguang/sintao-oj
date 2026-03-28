from app.core.config import load_settings
from app.llm.base import LLMClient
from app.llm.fallback import UnavailableLLMClient
from app.llm.openai_compatible import OpenAICompatibleLLMClient


def build_llm_client() -> LLMClient:
    settings = load_settings()
    if not settings.llm_enabled:
        return UnavailableLLMClient("llm is disabled")
    if settings.llm_provider == "openai_compatible":
        return OpenAICompatibleLLMClient(settings)
    return UnavailableLLMClient(f"unsupported llm provider: {settings.llm_provider}")
