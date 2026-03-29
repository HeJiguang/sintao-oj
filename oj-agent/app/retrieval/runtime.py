from app.retrieval.models import RetrievalQuery, RetrievalResult, RetrievedEvidence
from app.retrieval.routes import DenseRoute, LexicalRoute, PersonalizedRoute


class RetrievalRuntime:
    def __init__(self) -> None:
        self.lexical_route = LexicalRoute()
        self.dense_route = DenseRoute()
        self.personalized_route = PersonalizedRoute()

    def retrieve(self, query: RetrievalQuery) -> RetrievalResult:
        if not query.query_text.strip():
            return RetrievalResult(route_names=[], items=[])

        route_items = [
            ("lexical", self.lexical_route.retrieve(query)),
            ("dense", self.dense_route.retrieve(query)),
            ("personalized", self.personalized_route.retrieve(query)),
        ]
        route_names = [name for name, items in route_items if items]
        merged_items: list[RetrievedEvidence] = []
        for _name, items in route_items:
            merged_items.extend(items)

        merged_items.sort(key=lambda item: item.score or 0.0, reverse=True)
        return RetrievalResult(route_names=route_names, items=merged_items)
