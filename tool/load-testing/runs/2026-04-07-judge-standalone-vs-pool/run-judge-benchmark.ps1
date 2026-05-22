param(
    [ValidateSet("standalone", "pool")]
    [string]$Mode = "pool",

    [ValidateSet("warmup", "formal")]
    [string]$Phase = "formal",

    [int]$Vus = 10,

    [string]$Duration = "",

    [string]$BaseUrl = "",

    [switch]$SkipRestartJudge
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
$k6Script = Join-Path $scriptDir "judge-standalone-vs-pool.js"
if (!(Test-Path $k6Exe)) {
    throw "k6 executable not found: $k6Exe"
}

if (-not $SkipRestartJudge) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $scriptDir "restart-oj-judge.ps1") -Mode $Mode
    if ($LASTEXITCODE -ne 0) {
        throw "failed to restart oj-judge in mode $Mode"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryFile = Join-Path $resultsDir "judge-$Mode-$Phase-$timestamp.json"

Write-Host "Running judge sandbox benchmark..."
Write-Host "  Mode: $Mode"
Write-Host "  Phase: $Phase"
Write-Host "  BaseUrl: $BaseUrl"
Write-Host "  VUs: $Vus"
Write-Host "  Duration: $Duration"
Write-Host "  Summary: $summaryFile"

$env:BASE_URL = $BaseUrl
$env:MODE = $Mode
$env:VUS = [string]$Vus
$env:DURATION = $Duration
$env:PAUSE_SECONDS = [string]$DefaultPauseSeconds
$env:JUDGE_USER_ID = [string]$JudgeUserId
$env:JUDGE_QUESTION_ID = [string]$JudgeQuestionId
$env:JUDGE_PROGRAM_TYPE = [string]$JudgeProgramType
$env:JUDGE_DIFFICULTY = [string]$JudgeDifficulty
$env:JUDGE_TIME_LIMIT = [string]$JudgeTimeLimit
$env:JUDGE_SPACE_LIMIT = [string]$JudgeSpaceLimit
$env:JUDGE_USER_CODE = $JudgeUserCode
$env:JUDGE_INPUT_JSON = $JudgeInputJson
$env:JUDGE_OUTPUT_JSON = $JudgeOutputJson

& $k6Exe run $k6Script --summary-export $summaryFile
$exitCode = $LASTEXITCODE

Remove-Item Env:BASE_URL,Env:MODE,Env:VUS,Env:DURATION,Env:PAUSE_SECONDS,Env:JUDGE_USER_ID,Env:JUDGE_QUESTION_ID,Env:JUDGE_PROGRAM_TYPE,Env:JUDGE_DIFFICULTY,Env:JUDGE_TIME_LIMIT,Env:JUDGE_SPACE_LIMIT,Env:JUDGE_USER_CODE,Env:JUDGE_INPUT_JSON,Env:JUDGE_OUTPUT_JSON -ErrorAction SilentlyContinue

if ($exitCode -ne 0) {
    throw "k6 run failed with exit code $exitCode"
}

Write-Host ""
Write-Host "Judge sandbox benchmark finished."
Write-Host "Summary file: $summaryFile"
