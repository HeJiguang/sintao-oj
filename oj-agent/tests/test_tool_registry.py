from langchain_core.tools import StructuredTool

from app.runtime.data_sources import RequestBoundDataSources
from app.runtime.tools import build_default_tool_registry


class FakeKnowledgeRetriever:
    def search(self, *, request_context: dict, query: str | None, question_title: str | None, top_k: int = 3) -> list[dict]:
        del request_context, top_k
        return [
            {
                "source_id": "seed_hash_map",
                "title": "Hash Map Complement Pattern",
                "snippet": f"Use a hash map to store complements for {question_title or query}.",
                "source_name": "seed",
                "source_url": "https://example.test/hash-map",
                "license": "internal-seed",
                "score": 0.91,
            }
        ]


def test_default_tool_registry_uses_langchain_structured_tools():
    registry = build_default_tool_registry()

    assert registry.get("get_question_context")
    assert isinstance(registry.get("get_question_context"), StructuredTool)
    assert isinstance(registry.get("get_workspace_snapshot"), StructuredTool)
    assert isinstance(registry.get("get_judge_snapshot"), StructuredTool)


def test_default_knowledge_tool_returns_empty_items_when_provider_is_unconfigured():
    registry = build_default_tool_registry()

    result = registry.invoke(
        "retrieve_knowledge_evidence",
        managed_context={"request_context": {"question_title": "Two Sum"}},
        arguments={},
    )

    assert result["ok"] is True
    assert result["payload"]["items"] == []
    assert result["payload"]["provider_status"] == "unconfigured"


def test_knowledge_tool_returns_ranked_evidence_from_retriever():
    registry = build_default_tool_registry(
        RequestBoundDataSources(
            knowledge_retriever=FakeKnowledgeRetriever(),
        )
    )

    result = registry.invoke(
        "retrieve_knowledge_evidence",
        managed_context={
            "request_context": {
                "question_title": "Two Sum",
                "question_content": "Find two indices whose values add up to target.",
                "judge_result": "WA on sample #2",
                "user_message": "Why is this still wrong?",
            }
        },
        arguments={"query": "hash map complement duplicate values"},
    )

    assert result["ok"] is True
    assert result["payload"]["provider_status"] == "ok"
    assert result["payload"]["items"][0]["source_id"] == "seed_hash_map"
    assert result["payload"]["items"][0]["title"] == "Hash Map Complement Pattern"
