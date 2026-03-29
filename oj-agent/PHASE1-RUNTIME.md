# SynCode Unified Agent Kernel Phase 1 Runtime Notes

Date: 2026-03-29
Scope: `D:\Project\OnlineOJ\bite-oj-master\bite-oj-master\oj-agent`

## Summary

Phase 1 replaces the old toy orchestration path with a production-oriented unified agent kernel foundation.

The Python `oj-agent` runtime now owns:

- `SupervisorGraph`
- capability graph boundaries
- graph-native training-plan generation
- retrieval runtime foundations
- guardrail runtime foundations
- run trace, query ledger, and evaluation hooks
- structured write intents for Java-side persistence

The Java side remains responsible for business validation and persistence.

## Runtime Structure

### Unified runtime

- `app/runtime/engine.py`
  - chat runtime execution
  - training runtime execution
  - artifact recording for trace, query ledger, and evaluation
- `app/runtime/streaming.py`
  - runtime-native SSE chat streaming
- `app/runtime/models.py`
  - request, execution, evidence, guardrail, outcome, and write-intent models

### Graphs

- `app/graphs/supervisor_graph.py`
  - top-level routing for chat and training-plan requests
- `app/graphs/capabilities/tutor_graph.py`
  - tutoring path with retrieval and guardrail integration
- `app/graphs/capabilities/plan_graph.py`
  - graph-native training-plan path
- `app/graphs/capabilities/plan_nodes/*`
  - intake, gap analysis, candidate retrieval, draft planning, verification, repair, packaging

### Shared subsystems

- `app/retrieval/runtime.py`
  - normalized retrieval runtime
- `app/guardrails/runtime.py`
  - deterministic phase-1 guardrails
- `app/observability/trace.py`
  - run and node trace storage
- `app/observability/query_ledger.py`
  - query ledger storage
- `app/evaluation/hooks.py`
  - eval record builders

## Local Verification Commands

Run from repository root:

```powershell
pytest oj-agent/tests -q
```

Targeted Java compatibility checks:

```powershell
mvn -f oj-modules/oj-ai/pom.xml "-Dtest=PythonAgentClientTest,ChatFacadeServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl oj-modules/oj-friend -Dtest=TrainingAgentClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Runtime Contract Assumptions

- `/api/chat` and `/api/chat/detail` return the legacy `ChatResponse` shape expected by Java and frontend callers.
- `/api/chat/stream` now prepares state through the unified runtime before emitting SSE events.
- `/api/training/plan` returns the existing training-plan response contract used by `oj-friend`.
- write operations remain Java-owned; Python emits structured write intents only.

## Phase 1 Boundaries

Implemented now:

- unified runtime state
- supervisor-driven chat path
- runtime-native chat streaming
- graph-native plan generation
- retrieval, guardrail, trace, query-ledger, and eval foundations

Still intentionally lightweight in phase 1:

- dense retrieval route
- personalized retrieval route
- small-model verifier
- persistent trace/query/eval backends
- graph-native implementations for diagnose, recommend, review, and profile beyond boundary shells

## Notes

`oj-agent/README.md` was already in a deleted state in the working tree before this phase-1 closeout, so this note is used as the phase-1 runtime handoff document without restoring or overwriting that user change.
