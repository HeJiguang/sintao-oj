class InMemoryEvaluationStore:
    def __init__(self) -> None:
        self._records: list[dict] = []

    def append(self, record: dict) -> None:
        self._records.append(record)

    def list_records(self) -> list[dict]:
        return list(self._records)

    def clear(self) -> None:
        self._records.clear()


evaluation_store = InMemoryEvaluationStore()
