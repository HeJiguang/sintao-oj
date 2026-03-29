from app.observability.query_ledger import QueryLedger, QueryLedgerEntry
from app.observability.trace import InMemoryTraceStore, NodeTraceEvent, RunTrace


def test_in_memory_trace_store_records_run_and_node_events():
    store = InMemoryTraceStore()
    run = RunTrace(
        trace_id="trace-001",
        run_id="run-001",
        graph_name="supervisor_graph",
        task_type="chat",
        user_id="u-1",
    )

    store.record_run(run)
    store.record_node_event(
        NodeTraceEvent(
            trace_id="trace-001",
            run_id="run-001",
            graph_name="supervisor_graph",
            node_name="tutor_graph",
            status="succeeded",
        )
    )

    assert store.get_run("run-001").graph_name == "supervisor_graph"
    assert store.list_node_events("run-001")[0].node_name == "tutor_graph"


def test_query_ledger_records_query_summary():
    ledger = QueryLedger()
    entry = QueryLedgerEntry(
        trace_id="trace-002",
        run_id="run-002",
        user_id="u-2",
        task_type="training_plan",
        request_text="Generate my next training plan.",
        graph_path=["supervisor_graph", "plan_graph"],
        evidence_sources=["doc-1", "question-101"],
        output_type="plan_response",
        token_cost=123,
        latency_ms=456,
    )

    ledger.append(entry)

    assert ledger.list_entries()[0].output_type == "plan_response"
    assert ledger.list_entries()[0].token_cost == 123
