# 2026-04-07 Judge Standalone vs Pool

This run folder benchmarks the internal `oj-judge` execution path with two explicit modes:
- `standalone`: create and destroy one Docker container per judge request
- `pool`: reuse prewarmed sandbox containers from the Docker container pool

It targets:
- `POST http://127.0.0.1:9204/judge/doJudgeJavaCode`

One HTTP request equals one completed judge task, so `req/s` can be interpreted as completed-task throughput / TPS.

Files:
- `config.ps1`: default parameters and payload
- `restart-oj-judge.ps1`: restart local `oj-judge` with an explicit sandbox mode
- `judge-standalone-vs-pool.js`: k6 request logic
- `run-judge-benchmark.ps1`: restart + run k6 + export summary
- `compare-judge-results.ps1`: compare standalone and pool summaries
- `results/`: exported k6 summary files

Recommended workflow:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-judge-benchmark.ps1 -Mode standalone -Phase formal
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-judge-benchmark.ps1 -Mode pool -Phase formal
powershell -NoProfile -ExecutionPolicy Bypass -File .\compare-judge-results.ps1 -StandaloneSummary .\results\<standalone-summary>.json -PoolSummary .\results\<pool-summary>.json
```

Prerequisites:
- Docker Desktop must be running
- Docker TCP endpoint `127.0.0.1:2375` must be available to `oj-judge`
- Local `oj-judge` is started with the `local` profile
