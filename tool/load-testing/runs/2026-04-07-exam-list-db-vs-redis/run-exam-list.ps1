param(
    [ValidateSet("db", "redis")]
    [string]$Mode = "db",

    [ValidateSet("warmup", "formal")]
    [string]$Phase = "formal",

    [int]$Vus = 50,

    [string]$Duration = "",

    [string]$BaseUrl = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$loadTestingRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir "..\.."))
$configFile = Join-Path $scriptDir "config.ps1"
if (Test-Path $configFile) {
    . $configFile
}

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = if ($DefaultBaseUrl) { $DefaultBaseUrl } else { "http://127.0.0.1:19090" }
}

if ($Vus -le 0) {
    $Vus = if ($DefaultVus) { $DefaultVus } else { 50 }
}

$k6Exe = Join-Path $loadTestingRoot "bin\k6-v1.7.1-windows-amd64\k6.exe"
$k6Script = Join-Path $scriptDir "exam-list-db-vs-redis.js"
$resultsDir = Join-Path $scriptDir "results"

if (!(Test-Path $k6Exe)) {
    throw "k6 executable not found: $k6Exe"
}

if (!(Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Force $resultsDir | Out-Null
}

$endpoint = if ($Mode -eq "redis") {
    "/friend/exam/semiLogin/redis/list"
} else {
    "/friend/exam/semiLogin/list"
}

if ([string]::IsNullOrWhiteSpace($Duration)) {
    if ($Phase -eq "warmup") {
        $Duration = if ($DefaultWarmupDuration) { $DefaultWarmupDuration } else { "30s" }
    } else {
        $Duration = if ($DefaultFormalDuration) { $DefaultFormalDuration } else { "60s" }
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryFile = Join-Path $resultsDir "exam-list-$Mode-$Phase-$timestamp.json"

Write-Host "Running exam list load test..."
Write-Host "  Mode: $Mode"
Write-Host "  Phase: $Phase"
Write-Host "  BaseUrl: $BaseUrl"
Write-Host "  Endpoint: $endpoint"
Write-Host "  VUs: $Vus"
Write-Host "  Duration: $Duration"
Write-Host "  Summary: $summaryFile"

$env:BASE_URL = $BaseUrl
$env:ENDPOINT = $endpoint
$env:VUS = [string]$Vus
$env:DURATION = $Duration
$env:PAUSE_SECONDS = if ($DefaultPauseSeconds -ge 0) { [string]$DefaultPauseSeconds } else { "1" }

& $k6Exe run $k6Script --summary-export $summaryFile
$exitCode = $LASTEXITCODE

Remove-Item Env:BASE_URL -ErrorAction SilentlyContinue
Remove-Item Env:ENDPOINT -ErrorAction SilentlyContinue
Remove-Item Env:VUS -ErrorAction SilentlyContinue
Remove-Item Env:DURATION -ErrorAction SilentlyContinue
Remove-Item Env:PAUSE_SECONDS -ErrorAction SilentlyContinue

if ($exitCode -ne 0) {
    throw "k6 run failed with exit code $exitCode"
}

Write-Host ""
Write-Host "Load test finished."
Write-Host "Summary file: $summaryFile"
