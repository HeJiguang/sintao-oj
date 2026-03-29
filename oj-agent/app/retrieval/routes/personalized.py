from app.retrieval.models import RetrievedEvidence, RetrievalQuery


class PersonalizedRoute:
    route_name = "personalized"

    def retrieve(self, query: RetrievalQuery) -> list[RetrievedEvidence]:
        return []
