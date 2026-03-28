import pytest


@pytest.fixture(autouse=True)
def isolate_local_env_file(monkeypatch, tmp_path):
    monkeypatch.setenv("OJ_AGENT_ENV_FILE", str(tmp_path / ".env.test.local"))
