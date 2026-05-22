param(
    [string]$BaseUrl = "",
    [int]$Samples = 0,
    [int]$PollIntervalMs = 0,
    [int]$PollMaxAttempts = 0,
    [int]$TimeoutMs = 0,
    [int]$PauseMs = 0,
    [string]$WsUrl = "",
    [switch]$SkipEnsureSmoke
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir "config.ps1")

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = $DefaultBaseUrl
}
if ($Samples -le 0) {
    $Samples = $DefaultSamples
}
if ($PollIntervalMs -le 0) {
    $PollIntervalMs = $DefaultPollIntervalMs
}
if ($PollMaxAttempts -le 0) {
    $PollMaxAttempts = $DefaultPollMaxAttempts
}
if ($TimeoutMs -le 0) {
    $TimeoutMs = $DefaultTimeoutMs
}
if ($PauseMs -lt 0) {
    $PauseMs = 0
} elseif ($PauseMs -eq 0) {
    $PauseMs = $DefaultPauseMs
}

$runsDir = Split-Path -Parent $scriptDir
$bootstrapDir = Join-Path $runsDir $BootstrapRunName
$ensureScript = Join-Path $bootstrapDir "ensure-smoke-question.ps1"
$tokenScript = Join-Path $bootstrapDir "get-test-token.ps1"
$pythonScript = Join-Path $scriptDir "judge-result-latency.py"
$resultsDir = Join-Path $scriptDir "results"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryFile = Join-Path $resultsDir "judge-result-latency-$timestamp.json"

if (-not (Test-Path $ensureScript)) {
    throw "Bootstrap script not found: $ensureScript"
}
if (-not (Test-Path $tokenScript)) {
    throw "Token script not found: $tokenScript"
}
if (-not (Test-Path $pythonScript)) {
    throw "Python benchmark script not found: $pythonScript"
}
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir | Out-Null
}

if (-not $SkipEnsureSmoke) {
    Write-Host "Ensuring smoke question exists..."
    $previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
    $env:JAVA_TOOL_OPTIONS = "-Xms32m -Xmx128m"
    & powershell -NoProfile -ExecutionPolicy Bypass -File $ensureScript
    if ($null -eq $previousJavaToolOptions) {
        Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Smoke question bootstrap failed with exit code $LASTEXITCODE"
    }
}

Write-Host "Getting fresh test token..."
$tokenJson = & powershell -NoProfile -ExecutionPolicy Bypass -File $tokenScript -BaseUrl $BaseUrl
if ($LASTEXITCODE -ne 0) {
    throw "Failed to get test token"
}
$tokenInfo = $tokenJson | ConvertFrom-Json
if ([string]::IsNullOrWhiteSpace([string]$tokenInfo.token)) {
    throw "Token script returned empty token: $tokenJson"
}

Write-Host "Running paired result-latency benchmark..."
Write-Host "  Base URL: $BaseUrl"
Write-Host "  Samples: $Samples"
Write-Host "  Poll interval: $PollIntervalMs ms"
Write-Host "  Poll max attempts: $PollMaxAttempts"
Write-Host "  Timeout: $TimeoutMs ms"
if (-not [string]::IsNullOrWhiteSpace($WsUrl)) {
    Write-Host "  WS URL override: $WsUrl"
}
Write-Host "  Summary: $summaryFile"

$pythonArgs = @(
    $pythonScript,
    "--base-url", $BaseUrl,
    "--token", $tokenInfo.token,
    "--question-id", $SmokeQuestionId,
    "--program-type", $SmokeQuestionProgramType,
    "--user-code", $SmokeQuestionUserCode,
    "--samples", $Samples,
    "--poll-interval-ms", $PollIntervalMs,
    "--poll-max-attempts", $PollMaxAttempts,
    "--timeout-ms", $TimeoutMs,
    "--pause-ms", $PauseMs,
    "--output", $summaryFile
)

if (-not [string]::IsNullOrWhiteSpace($WsUrl)) {
    $pythonArgs += @("--ws-url", $WsUrl)
}

& python @pythonArgs

if ($LASTEXITCODE -ne 0) {
    throw "Result-latency benchmark failed with exit code $LASTEXITCODE"
}

Write-Host "Result-latency benchmark completed."
Write-Host "Summary file: $summaryFile"
