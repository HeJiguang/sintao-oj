from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
import glob
import re

from app.core.config import load_settings


TOKEN_PATTERN = re.compile(r"[a-zA-Z0-9_+-]+")
HEADER_PATTERN = re.compile(r"^#\s+(.*)$", re.MULTILINE)


@dataclass(frozen=True)
class KnowledgeDocument:
    source: str
    title: str
    content: str
    tokens: tuple[str, ...]


@dataclass(frozen=True)
class KnowledgeHit:
    source: str
    title: str
    excerpt: str
    score: float


def _tokenize(text: str) -> tuple[str, ...]:
    return tuple(token.lower() for token in TOKEN_PATTERN.findall(text))


def _read_title(content: str, fallback: str) -> str:
    match = HEADER_PATTERN.search(content)
    if match:
        return match.group(1).strip()
    return fallback


@lru_cache(maxsize=8)
def _load_documents(doc_globs: tuple[str, ...]) -> tuple[KnowledgeDocument, ...]:
    docs: list[KnowledgeDocument] = []
    for pattern in doc_globs:
        for matched in glob.glob(pattern, recursive=True):
            path = Path(matched)
            if not path.exists() or not path.is_file():
                continue
            content = path.read_text(encoding="utf-8", errors="ignore").strip()
            if not content:
                continue
            title = _read_title(content, path.stem.replace("-", " ").replace("_", " "))
            docs.append(
                KnowledgeDocument(
                    source=str(path),
                    title=title,
                    content=content,
                    tokens=_tokenize(title + "\n" + content),
                )
            )
    return tuple(docs)


def _excerpt(doc: KnowledgeDocument, max_chars: int) -> str:
    text = re.sub(r"\s+", " ", doc.content).strip()
    return text[:max_chars]


class KeywordKnowledgeRetriever:
    def __init__(self) -> None:
        settings = load_settings()
        self.enabled = settings.rag_enabled
        self.doc_globs = settings.rag_doc_globs
        self.top_k = settings.rag_top_k
        self.max_snippet_chars = settings.rag_max_snippet_chars

    def retrieve(self, query: str) -> list[KnowledgeHit]:
        if not self.enabled:
            return []

        query_tokens = set(_tokenize(query))
        if not query_tokens:
            return []

        scored: list[KnowledgeHit] = []
        for doc in _load_documents(self.doc_globs):
            doc_tokens = set(doc.tokens)
            overlap = query_tokens & doc_tokens
            if not overlap:
                continue
            title_tokens = set(_tokenize(doc.title))
            title_hits = len(query_tokens & title_tokens)
            score = float(len(overlap) + title_hits * 2)
            scored.append(
                KnowledgeHit(
                    source=doc.source,
                    title=doc.title,
                    excerpt=_excerpt(doc, self.max_snippet_chars),
                    score=score,
                )
            )

        scored.sort(key=lambda item: item.score, reverse=True)
        return scored[: self.top_k]
