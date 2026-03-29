from app.retrieval.models import RetrievedEvidence, RetrievalQuery


class DenseRoute:
    route_name = "dense"

    def retrieve(self, query: RetrievalQuery) -> list[RetrievedEvidence]:
        return []
