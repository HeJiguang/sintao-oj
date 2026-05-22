from dataclasses import dataclass
from pathlib import Path
import os


@dataclass(frozen=True)
class AgentSettings:
    llm_provider: str = "deepseek"
    llm_api_key: str | None = None
    llm_model: str = "deepseek-chat"
    llm_base_url: str = "https://api.deepseek.com"
    llm_temperature: float = 0.2
    llm_max_tokens: int = 1200
    qdrant_url: str | None = None
    qdrant_collection: str = "oj_agent_knowledge"
    qdrant_vector_size: int = 64
    qdrant_top_k: int = 3

    @property
    def real_llm_enabled(self) -> bool:
        return bool(self.llm_api_key and self.llm_provider == "deepseek")

    @property
    def qdrant_enabled(self) -> bool:
        return bool(self.qdrant_url)


def load_settings() -> AgentSettings:
    _load_env_file()
    return AgentSettings(
        llm_provider=os.getenv("OJ_AGENT_LLM_PROVIDER", "deepseek"),
        llm_api_key=os.getenv("OJ_AGENT_LLM_API_KEY"),
        llm_model=os.getenv("OJ_AGENT_LLM_MODEL", "deepseek-chat"),
        llm_base_url=os.getenv("OJ_AGENT_LLM_BASE_URL", "https://api.deepseek.com"),
        llm_temperature=float(os.getenv("OJ_AGENT_LLM_TEMPERATURE", "0.2")),
        llm_max_tokens=int(os.getenv("OJ_AGENT_LLM_MAX_TOKENS", "1200")),
        qdrant_url=os.getenv("OJ_AGENT_QDRANT_URL"),
        qdrant_collection=os.getenv("OJ_AGENT_QDRANT_COLLECTION", "oj_agent_knowledge"),
        qdrant_vector_size=int(os.getenv("OJ_AGENT_QDRANT_VECTOR_SIZE", "64")),
        qdrant_top_k=int(os.getenv("OJ_AGENT_QDRANT_TOP_K", "3")),
    )


def _load_env_file() -> None:
    env_path = Path(__file__).resolve().parents[1] / ".env.local"
    if not env_path.exists():
        return

    for line in env_path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        key = key.strip()
        value = value.strip()
        if key and key not in os.environ:
            os.environ[key] = value
