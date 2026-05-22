from __future__ import annotations

from dataclasses import dataclass
import hashlib
import json
import math
from pathlib import Path
from typing import Protocol

from app.config import AgentSettings, load_settings


class KnowledgeRetriever(Protocol):
    def search(
        self,
        *,
        request_context: dict,
        query: str | None,
        question_title: str | None,
        top_k: int = 3,
    ) -> list[dict]: ...


@dataclass(frozen=True)
class UnavailableKnowledgeRetriever:
    message: str

    def search(
        self,
        *,
        request_context: dict,
        query: str | None,
        question_title: str | None,
        top_k: int = 3,
    ) -> list[dict]:
        del request_context, query, question_title, top_k
        raise RuntimeError(self.message)


def build_default_knowledge_retriever(settings: AgentSettings | None = None) -> KnowledgeRetriever | None:
    resolved = settings or load_settings()
    if not resolved.qdrant_enabled:
        return None
    try:
        return QdrantKnowledgeRetriever(settings=resolved)
    except ImportError as exc:
        return UnavailableKnowledgeRetriever(
            "qdrant-client is required when OJ_AGENT_QDRANT_URL is configured"
        )


def load_seed_documents(path: Path | None = None) -> list[dict]:
    resolved = path or _default_seed_path()
    payload = json.loads(resolved.read_text(encoding="utf-8"))
    if not isinstance(payload, list):
        raise ValueError("seed_documents.json must contain a JSON list")
    return payload


def build_query_text(*, request_context: dict, query: str | None, question_title: str | None) -> str:
    parts = [
        query or "",
        question_title or request_context.get("question_title") or "",
        request_context.get("judge_result") or "",
        request_context.get("user_message") or "",
        request_context.get("question_content") or "",
    ]
    return "\n".join(part for part in parts if part)


def build_document_text(document: dict) -> str:
    return "\n".join(
        [
            str(document.get("title") or ""),
            str(document.get("snippet") or ""),
            str(document.get("topic") or ""),
            " ".join(document.get("tags") or []),
        ]
    )


def build_hashed_embedding(text: str, size: int) -> list[float]:
    vector = [0.0] * size
    for token in _tokenize(text):
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        slot = int.from_bytes(digest[:4], "big") % size
        sign = 1.0 if digest[4] % 2 == 0 else -1.0
        vector[slot] += sign

    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return vector
    return [value / norm for value in vector]


class QdrantKnowledgeRetriever:
    def __init__(self, settings: AgentSettings | None = None, client=None) -> None:
        self.settings = settings or load_settings()
        self._client = client or self._build_client()

    def search(
        self,
        *,
        request_context: dict,
        query: str | None,
        question_title: str | None,
        top_k: int = 3,
    ) -> list[dict]:
        search_text = build_query_text(
            request_context=request_context,
            query=query,
            question_title=question_title,
        )
        result = self._client.query_points(
            collection_name=self.settings.qdrant_collection,
            query=build_hashed_embedding(search_text, self.settings.qdrant_vector_size),
            limit=max(1, top_k),
            with_payload=True,
        )

        normalized: list[dict] = []
        for item in result.points:
            payload = dict(item.payload or {})
            normalized.append(
                {
                    "source_id": payload.get("id") or payload.get("source_id") or str(getattr(item, "id", "")),
                    "title": payload.get("title") or "Knowledge Evidence",
                    "snippet": payload.get("snippet") or "",
                    "source_name": payload.get("source_name") or "qdrant",
                    "source_url": payload.get("source_url") or "",
                    "license": payload.get("license") or "unknown",
                    "score": float(getattr(item, "score", 0.0) or 0.0),
                }
            )
        return normalized

    def seed_documents(self, documents: list[dict]) -> int:
        models = _import_qdrant_models()
        self._client.recreate_collection(
            collection_name=self.settings.qdrant_collection,
            vectors_config=models.VectorParams(
                size=self.settings.qdrant_vector_size,
                distance=models.Distance.COSINE,
            ),
        )
        points = [
            models.PointStruct(
                id=_point_id(str(document["id"])),
                vector=build_hashed_embedding(build_document_text(document), self.settings.qdrant_vector_size),
                payload=document,
            )
            for document in documents
        ]
        self._client.upsert(collection_name=self.settings.qdrant_collection, points=points)
        return len(points)

    def _build_client(self):
        qdrant_client = _import_qdrant_client()
        return qdrant_client.QdrantClient(url=self.settings.qdrant_url, check_compatibility=False)


def _default_seed_path() -> Path:
    return Path(__file__).resolve().parents[2] / "resources" / "knowledge" / "seed_documents.json"


def _tokenize(text: str) -> list[str]:
    normalized = "".join(char.lower() if char.isalnum() else " " for char in text)
    return [token for token in normalized.split() if token]


def _import_qdrant_client():
    import qdrant_client

    return qdrant_client


def _import_qdrant_models():
    from qdrant_client.http import models

    return models


def _point_id(source_id: str) -> int:
    return int(hashlib.sha256(source_id.encode("utf-8")).hexdigest()[:16], 16)
