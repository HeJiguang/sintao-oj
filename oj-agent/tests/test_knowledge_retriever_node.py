from pathlib import Path
import sys


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from app.graph.nodes.knowledge_retriever import knowledge_retriever_node  # noqa: E402


def test_knowledge_retriever_returns_ranked_snippets(monkeypatch, tmp_path: Path):
    doc = tmp_path / "binary-search.md"
    doc.write_text(
        "\n".join(
            [
                "# Binary Search",
                "Binary search works on sorted arrays.",
                "It is useful for boundary-finding and lower-bound problems.",
            ]
        ),
        encoding="utf-8",
    )

    monkeypatch.setenv("OJ_AGENT_RAG_ENABLED", "true")
    monkeypatch.setenv("OJ_AGENT_RAG_DOC_GLOBS", str(doc))
    monkeypatch.setenv("OJ_AGENT_RAG_TOP_K", "2")

    result = knowledge_retriever_node(
        {
            "trace_id": "trace-rag-1",
            "user_id": "u-1",
            "question_title": "Binary Search Template",
            "user_message": "How should I reason about lower bound binary search?",
            "status_events": [],
        }
    )

    assert result["knowledge_hits"]
    assert result["knowledge_hits"][0]["source"].endswith("binary-search.md")
    assert "Binary search works on sorted arrays." in result["knowledge_hits"][0]["excerpt"]
    assert result["status_events"][-1]["node"] == "knowledge_retriever"
