from pydantic import BaseModel, Field


class QueryLedgerEntry(BaseModel):
    trace_id: str
    run_id: str
    user_id: str
    task_type: str
    request_text: str
    graph_path: list[str] = Field(default_factory=list)
    evidence_sources: list[str] = Field(default_factory=list)
    output_type: str
    token_cost: int = 0
    latency_ms: int = 0


class QueryLedger:
    def __init__(self) -> None:
        self._entries: list[QueryLedgerEntry] = []

    def append(self, entry: QueryLedgerEntry) -> None:
        self._entries.append(entry)

    def list_entries(self) -> list[QueryLedgerEntry]:
        return list(self._entries)

    def clear(self) -> None:
        self._entries.clear()


query_ledger = QueryLedger()
