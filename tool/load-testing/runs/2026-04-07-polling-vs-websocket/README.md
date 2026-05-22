# 2026-04-07 Polling vs WebSocket

This run folder benchmarks judge-result delivery latency for the same async submission.

It measures two result-delivery paths in parallel for each `requestId`:
- polling: `/friend/user/question/exe/result`
- websocket: `/friend/ws/judge/result`

Files:
- `config.ps1`: default parameters for this benchmark
- `run-result-latency.ps1`: bootstraps smoke data, gets a test token, and runs the benchmark
- `judge-result-latency.py`: Python asyncio paired-latency measurement script
- `compare-result-latency.ps1`: prints one or more summary files as a comparison table
- `results/`: summary JSON files for this benchmark only

Recommended workflow:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-result-latency.ps1 -Samples 10
powershell -NoProfile -ExecutionPolicy Bypass -File .\compare-result-latency.ps1 -Summaries .\results\judge-result-latency-*.json
```

Notes:
- This run reuses the smoke question bootstrap and token bootstrap from `2026-04-07-submit-sync-vs-rabbit`.
- Each sample submits once, then starts polling and websocket listeners for the same `requestId`.
- Polling follows the real frontend behavior: immediate first query, then `1500ms` interval, up to `6` attempts by default.
