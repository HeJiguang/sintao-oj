# 2026-04-07 Submit Sync vs Rabbit

This run folder is for benchmarking:
- `/friend/user/question/submit`
- `/friend/user/question/rabbit/submit`

It includes:
- smoke question bootstrap
- test-user token bootstrap
- k6 benchmark scripts
- result comparison script
- result files for this benchmark only

Suggested workflow:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ensure-smoke-question.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-submit-benchmark.ps1 -Mode sync -Phase formal
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-submit-benchmark.ps1 -Mode async -Phase formal
```
