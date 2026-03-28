from pathlib import Path

from app.core.config import load_settings


def test_load_settings_reads_local_env_file(tmp_path: Path, monkeypatch):
    env_file = tmp_path / ".env.local"
    env_file.write_text(
        "\n".join(
            [
                "OJ_AGENT_LLM_PROVIDER=openai_compatible",
                "OJ_AGENT_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1",
                "OJ_AGENT_LLM_API_KEY=test-key",
                "OJ_AGENT_CHAT_MODEL=qwen-turbo",
                "OJ_AGENT_TRAINING_MODEL=qwen-plus",
                "OJ_AGENT_NACOS_ENABLED=true",
                "OJ_AGENT_NACOS_SERVICE_NAME=oj-agent",
            ]
        ),
        encoding="utf-8",
    )

    monkeypatch.setenv("OJ_AGENT_ENV_FILE", str(env_file))
    monkeypatch.delenv("OJ_AGENT_LLM_API_KEY", raising=False)

    settings = load_settings()

    assert settings.llm_enabled is True
    assert settings.llm_api_key == "test-key"
    assert settings.chat_model == "qwen-turbo"
    assert settings.training_model == "qwen-plus"
    assert settings.nacos_enabled is True


def test_load_settings_reads_nacos_bootstrap_config(monkeypatch):
    nacos_yaml = "\n".join(
        [
            "server:",
            "  port: 8015",
            "llm:",
            "  provider: openai_compatible",
            "  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1",
            "  api-key: nacos-test-key",
            "  chat-model: qwen-turbo",
            "  training-model: qwen-plus",
            "rag:",
            "  enabled: true",
            "  top-k: 3",
            "  max-snippet-chars: 500",
        ]
    )

    monkeypatch.delenv("OJ_AGENT_LLM_API_KEY", raising=False)
    monkeypatch.setenv("OJ_AGENT_NACOS_SERVER_ADDR", "http://localhost:8848")
    monkeypatch.setenv("OJ_AGENT_NACOS_NAMESPACE", "test-namespace")
    monkeypatch.setenv("OJ_AGENT_NACOS_CONFIG_ENABLED", "true")
    monkeypatch.setenv("OJ_AGENT_NACOS_CONFIG_DATA_ID", "oj-agent-local.yaml")

    from app.core import config as config_module  # noqa: WPS433

    monkeypatch.setattr(
        config_module,
        "_load_nacos_config",
        lambda: {"raw": nacos_yaml},
    )

    settings = load_settings()

    assert settings.llm_enabled is True
    assert settings.llm_api_key == "nacos-test-key"
    assert settings.chat_model == "qwen-turbo"
    assert settings.training_model == "qwen-plus"
    assert settings.nacos_port == 8015
    assert settings.rag_enabled is True
    assert settings.rag_top_k == 3
