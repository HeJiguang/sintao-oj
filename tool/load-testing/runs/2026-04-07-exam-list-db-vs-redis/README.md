# 2026-04-07 Exam List DB vs Redis

This folder contains one self-contained benchmark run for:
- `/friend/exam/semiLogin/list`
- `/friend/exam/semiLogin/redis/list`

Files:
- `config.ps1`: default parameters for this benchmark
- `run-exam-list.ps1`: runs the benchmark
- `compare-exam-list-results.ps1`: compares two k6 summary JSON files
- `exam-list-db-vs-redis.js`: k6 request logic
- `results/`: exported k6 summary files

Common commands:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-exam-list.ps1 -Mode db -Phase formal
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-exam-list.ps1 -Mode redis -Phase formal
```
