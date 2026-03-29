from app.retrieval.models import RetrievedEvidence, RetrievalQuery
from app.retrieval.runtime import RetrievalRuntime


def test_retrieval_runtime_returns_normalized_evidence_from_routes(monkeypatch):
    runtime = RetrievalRuntime()

    monkeypatch.setattr(
        runtime.lexical_route,
        "retrieve",
        lambda query: [
            RetrievedEvidence(
                evidence_id="lex-1",
                route_name="lexical",
                source_type="knowledge_doc",
                source_id="doc-1",
                title="Hash Map Patterns",
                snippet="Use a hash map to track complements.",
                score=0.92,
            )
        ],
    )
    monkeypatch.setattr(runtime.dense_route, "retrieve", lambda query: [])
    monkeypatch.setattr(runtime.personalized_route, "retrieve", lambda query: [])

    result = runtime.retrieve(
        RetrievalQuery(
            query_text="two sum wrong answer",
            task_type="chat",
        )
    )

    assert result.route_names == ["lexical"]
    assert len(result.items) == 1
    assert result.items[0].source_id == "doc-1"
    assert result.items[0].route_name == "lexical"


def test_retrieval_runtime_returns_empty_result_for_blank_query():
    runtime = RetrievalRuntime()

    result = runtime.retrieve(
        RetrievalQuery(
            query_text="   ",
            task_type="chat",
        )
    )

    assert result.route_names == []
    assert result.items == []
