# 2026-03-29 Phase1 Agent Kernel Foundation

## This Round Goal

Finish the first production-oriented foundation of the unified SynCode agent kernel inside `oj-agent` without regressing the existing Java contracts.

The scope of this round was:

- replace the old chat entrypoint with a supervisor-driven runtime
- move training-plan generation into a graph-native path
- make streaming runtime-native
- introduce retrieval, guardrail, observability, and evaluation foundations
- keep Java-side compatibility for `oj-ai` and `oj-friend`

## What Is Live In Phase 1

### Unified runtime foundation

- [engine.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/runtime/engine.py)
- [models.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/runtime/models.py)
- [streaming.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/runtime/streaming.py)
- [supervisor_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/supervisor_graph.py)

These files now define the unified request/execution/evidence/guardrail/outcome state, execute chat and training requests through the supervisor graph, and record runtime artifacts.

### Capability graph boundaries

- [tutor_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/tutor_graph.py)
- [diagnose_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/diagnose_graph.py)
- [recommend_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/recommend_graph.py)
- [plan_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/plan_graph.py)
- [review_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/review_graph.py)
- [profile_graph.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/profile_graph.py)

The tutor path is the first real runtime-backed capability graph.

The other capability graphs are now explicit boundaries rather than being mixed into one toy chain.

### Graph-native plan generation

- [training.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/api/training.py)
- [training_planner.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/services/training_planner.py)
- [plan_nodes](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/graphs/capabilities/plan_nodes)

Training-plan generation now runs through explicit graph stages:

- intake
- gap analysis
- candidate retrieval
- draft planning
- verification
- repair
- packaging

This is the first phase that makes plan verification and repair explicit in the runtime.

### Retrieval, guardrail, and observability foundations

- [runtime.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/retrieval/runtime.py)
- [runtime.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/guardrails/runtime.py)
- [trace.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/observability/trace.py)
- [query_ledger.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/observability/query_ledger.py)
- [hooks.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/evaluation/hooks.py)
- [store.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/evaluation/store.py)

These modules give phase 1:

- normalized evidence objects
- retrieval route boundaries
- deterministic guardrail checks
- run and node trace recording
- query ledger recording
- evaluation hook recording

### Runtime-native streaming

- [chat.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/api/chat.py)
- [streaming.py](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/oj-agent/app/runtime/streaming.py)

`/api/chat/stream` no longer contains the broken placeholder flow.

It now prepares state through the unified runtime, emits runtime status events, streams answer deltas, and records trace/query/eval artifacts at completion.

## What Stayed Compatible

- Python chat sync/detail response shape stayed compatible with the existing callers.
- Python training-plan response shape stayed compatible with `oj-friend`.
- Java compatibility tests for `oj-ai` and `oj-friend` still pass against the updated phase-1 runtime.

## What Is Still Placeholder

The following are intentionally not “enterprise-complete” yet:

- dense retrieval route is still a shell
- personalized retrieval route is still a shell
- guardrails are deterministic only and do not yet use a verifier model
- trace, query ledger, and evaluation stores are still in-memory implementations
- diagnose/recommend/review/profile graphs are boundary shells, not full graph-native capability implementations

This is acceptable for phase 1 because the goal was the unified kernel foundation, not the final production maturity of every subsystem.

## Verification

### Python

- `pytest oj-agent/tests -q`
  - phase-1 completion target: all tests pass

### Java

- `mvn -f oj-modules/oj-ai/pom.xml "-Dtest=PythonAgentClientTest,ChatFacadeServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl oj-modules/oj-friend -Dtest=TrainingAgentClientTest "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Next Increment

The next increment should focus on phase-2 quality hardening rather than another large structural rewrite:

- turn retrieval into real hybrid recall plus reranking
- add coverage scoring and stronger hallucination interception
- persist trace/query/eval artifacts outside memory
- make diagnose/recommend/review/profile graphs truly graph-native
- add more deployment-oriented rate limiting and replay tooling
