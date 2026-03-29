from app.retrieval.keyword_retriever import KeywordKnowledgeRetriever
from app.retrieval.models import RetrievedEvidence, RetrievalQuery


class LexicalRoute:
    route_name = "lexical"

    def __init__(self) -> None:
        self._retriever = KeywordKnowledgeRetriever()

    def retrieve(self, query: RetrievalQuery) -> list[RetrievedEvidence]:
        hits = self._retriever.retrieve(query.query_text)
        return [
            RetrievedEvidence(
                evidence_id=f"lexical-{index}",
                route_name=self.route_name,
                source_type="knowledge_doc",
                source_id=hit.source,
                title=hit.title,
                snippet=hit.excerpt,
                score=hit.score,
            )
            for index, hit in enumerate(hits, start=1)
        ]
