import pytest

from app.config import AgentSettings
from app.runtime.llm import DeepSeekLLM, build_default_llm


def test_build_default_llm_requires_real_llm_config():
    settings = AgentSettings(llm_api_key=None)

    with pytest.raises(RuntimeError, match="Real LLM configuration is required"):
        build_default_llm(settings)


def test_build_default_llm_uses_deepseek_llm_when_key_present():
    settings = AgentSettings(
        llm_provider="deepseek",
        llm_api_key="test-key",
        llm_model="deepseek-chat",
        llm_base_url="https://api.deepseek.com",
    )

    llm = build_default_llm(settings)

    assert isinstance(llm, DeepSeekLLM)


def test_llm_client_only_exposes_generic_runtime_methods():
    settings = AgentSettings(
        llm_provider="deepseek",
        llm_api_key="test-key",
        llm_model="deepseek-chat",
        llm_base_url="https://api.deepseek.com",
    )

    llm = build_default_llm(settings)

    assert hasattr(llm, "invoke_structured")
    assert hasattr(llm, "stream_text")
    assert not hasattr(llm, "interpret_context")
    assert not hasattr(llm, "plan_next_action")
    assert not hasattr(llm, "respond")
