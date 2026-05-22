param(
    [ValidateSet("sync", "async")]
    [string]$Mode = "sync",

    [ValidateSet("warmup", "formal")]
    [string]$Phase = "formal",

    [int]$Vus = 20,

    [string]$Duration = "",

    [string]$BaseUrl = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$loadTestingRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir "..\.."))
. (Join-Path $scriptDir "config.ps1")

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = $DefaultBaseUrl
}
if ($Vus -le 0) {
    $Vus = $DefaultVus
}
if ([string]::IsNullOrWhiteSpace($Duration)) {
    $Duration = if ($Phase -eq "warmup") { $DefaultWarmupDuration } else { $DefaultFormalDuration }
}

$resultsDir = Join-Path $scriptDir "results"
if (!(Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Force $resultsDir | Out-Null
}

$k6Exe = Join-Path $loadTestingRoot "bin\k6-v1.7.1-windows-amd64\k6.exe"
$k6Script = Join-Path $scriptDir "submit-sync-vs-rabbit.js"
if (!(Test-Path $k6Exe)) {
    throw "k6 executable not found: $k6Exe"
}

$tokenJson = powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $scriptDir "get-test-token.ps1") -BaseUrl $BaseUrl
if ($LASTEXITCODE -ne 0) {
    throw "failed to get test token"
}
$tokenObj = $tokenJson | ConvertFrom-Json
if (-not $tokenObj.token) {
    throw "token bootstrap returned empty token"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryFile = Join-Path $resultsDir "submit-$Mode-$Phase-$timestamp.json"

Write-Host "Running submit benchmark..."
Write-Host "  Mode: $Mode"
Write-Host "  Phase: $Phase"
Write-Host "  BaseUrl: $BaseUrl"
Write-Host "  VUs: $Vus"
Write-Host "  Duration: $Duration"
Write-Host "  Question ID: $SmokeQuestionId"
Write-Host "  Summary: $summaryFile"

$env:BASE_URL = $BaseUrl
$env:MODE = $Mode
$env:AUTH_TOKEN = [string]$tokenObj.token
$env:QUESTION_ID = [string]$SmokeQuestionId
$env:VUS = [string]$Vus
$env:DURATION = $Duration
$env:PAUSE_SECONDS = [string]$DefaultPauseSeconds
$env:USER_CODE = $SmokeQuestionDefaultCode

& $k6Exe run $k6Script --summary-export $summaryFile
$exitCode = $LASTEXITCODE

Remove-Item Env:BASE_URL,Env:MODE,Env:AUTH_TOKEN,Env:QUESTION_ID,Env:VUS,Env:DURATION,Env:PAUSE_SECONDS,Env:USER_CODE -ErrorAction SilentlyContinue

if ($exitCode -ne 0) {
    throw "k6 run failed with exit code $exitCode"
}

Write-Host ""
Write-Host "Load test finished."
Write-Host "Summary file: $summaryFile"
