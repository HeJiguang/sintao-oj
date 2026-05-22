param(
    [ValidateSet("standalone", "pool", "auto")]
    [string]$Mode = "auto"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDir "..\..\..\.."))
$runtimeRoot = Join-Path $repoRoot "logs\local-runtime"
$localEnvScript = Join-Path $repoRoot "deploy\dev\local\local-env.ps1"
$stdout = Join-Path $runtimeRoot "oj-judge.out.log"
$stderr = Join-Path $runtimeRoot "oj-judge.err.log"
$pidFile = Join-Path $runtimeRoot "oj-judge.pid"

if (-not (Test-Path $runtimeRoot)) {
    New-Item -ItemType Directory -Path $runtimeRoot | Out-Null
}

. $localEnvScript
$env:SANDBOX_EXECUTION_MODE = $Mode
$judgePort = if ($env:OJ_JUDGE_PORT) { [int]$env:OJ_JUDGE_PORT } else { 9204 }

function Resolve-CommandPath {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Candidates
    )

    foreach ($candidate in $Candidates) {
        $command = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    throw "Cannot find command: $($Candidates -join ', ')"
}

function Stop-JudgeProcess {
    if (Test-Path $pidFile) {
        $pidValue = Get-Content $pidFile | Select-Object -First 1
        if ($pidValue -and (Get-Process -Id $pidValue -ErrorAction SilentlyContinue)) {
            & taskkill /PID $pidValue /T /F | Out-Null
            Write-Host ("[oj-judge] stopped by pid={0}" -f $pidValue) -ForegroundColor Yellow
        }
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    }

    $listener = Get-NetTCPConnection -LocalPort $judgePort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        & taskkill /PID $listener.OwningProcess /T /F | Out-Null
        Write-Host ("[oj-judge] stopped by port {0}, pid={1}" -f $judgePort, $listener.OwningProcess) -ForegroundColor Yellow
    }
}

function Wait-ForJudgeReady {
    $deadline = (Get-Date).AddSeconds(180)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri ("http://127.0.0.1:{0}/doc.html" -f $judgePort) -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host ("[oj-judge] ready -> http://127.0.0.1:{0}/doc.html" -f $judgePort) -ForegroundColor Green
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "[oj-judge] not ready within 180s"
}

$mvn = Resolve-CommandPath -Candidates @("mvn.cmd", "mvn")
Stop-JudgeProcess

if (Test-Path $stdout) { Remove-Item -LiteralPath $stdout -Force -ErrorAction SilentlyContinue }
if (Test-Path $stderr) { Remove-Item -LiteralPath $stderr -Force -ErrorAction SilentlyContinue }

$process = Start-Process `
    -FilePath $mvn `
    -ArgumentList @("-f", "oj-modules/oj-judge/pom.xml", "-Dmaven.repo.local=temp/m2-repo", "spring-boot:run", "-Dspring-boot.run.profiles=local", "-Dspring-boot.run.arguments=--server.port=$judgePort") `
    -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -PassThru `
    -WindowStyle Hidden

Set-Content -LiteralPath $pidFile -Value $process.Id
Write-Host ("[oj-judge] started pid={0}, sandbox.execution.mode={1}" -f $process.Id, $Mode) -ForegroundColor Green

Wait-ForJudgeReady
