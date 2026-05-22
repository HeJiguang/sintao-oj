$ErrorActionPreference = "Stop"

$frontendWebPort = if ($env:FRONTEND_WEB_PORT) { [int]$env:FRONTEND_WEB_PORT } else { 4000 }
$frontendAppPort = if ($env:FRONTEND_APP_PORT) { [int]$env:FRONTEND_APP_PORT } else { 4201 }
$frontendAdminPort = if ($env:FRONTEND_ADMIN_PORT) { [int]$env:FRONTEND_ADMIN_PORT } else { 4002 }
$agentPort = if ($env:OJ_AGENT_PORT) { [int]$env:OJ_AGENT_PORT } else { 8016 }
$systemPort = if ($env:OJ_SYSTEM_PORT) { [int]$env:OJ_SYSTEM_PORT } else { 9201 }
$friendPort = if ($env:OJ_FRIEND_PORT) { [int]$env:OJ_FRIEND_PORT } else { 9202 }
$jobPort = if ($env:OJ_JOB_PORT) { [int]$env:OJ_JOB_PORT } else { 9203 }
$judgePort = if ($env:OJ_JUDGE_PORT) { [int]$env:OJ_JUDGE_PORT } else { 9204 }

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path $PSScriptRoot))
$runtimeRoot = Join-Path $repoRoot "logs\local-runtime"

$targets = @(
    @{ Name = "frontend-web"; Port = $frontendWebPort; Url = "http://127.0.0.1:$frontendWebPort" },
    @{ Name = "frontend-app"; Port = $frontendAppPort; Url = "http://127.0.0.1:$frontendAppPort/app" },
    @{ Name = "frontend-admin"; Port = $frontendAdminPort; Url = "http://127.0.0.1:$frontendAdminPort/admin" },
    @{ Name = "oj-agent"; Port = $agentPort; Url = "http://127.0.0.1:$agentPort/docs" },
    @{ Name = "oj-gateway"; Port = 19090; Url = "http://127.0.0.1:19090/friend/doc.html" },
    @{ Name = "oj-system"; Port = $systemPort; Url = "http://127.0.0.1:$systemPort/doc.html" },
    @{ Name = "oj-friend"; Port = $friendPort; Url = "http://127.0.0.1:$friendPort/doc.html" },
    @{ Name = "oj-job"; Port = $jobPort; Url = "http://127.0.0.1:$jobPort/" },
    @{ Name = "oj-judge"; Port = $judgePort; Url = "http://127.0.0.1:$judgePort/doc.html" }
)

$rows = foreach ($target in $targets) {
    $pidFile = Join-Path $runtimeRoot ("{0}.pid" -f $target.Name)
    $pidValue = if (Test-Path $pidFile) { (Get-Content $pidFile | Select-Object -First 1) } else { "" }
    $listener = Get-NetTCPConnection -LocalPort $target.Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1

    $status = "DOWN"
    if ($listener) {
        $status = "LISTEN"
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $target.Url -TimeoutSec 3
            $status = "HTTP $($response.StatusCode)"
        }
        catch {
            if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -lt 500) {
                $status = "HTTP $([int]$_.Exception.Response.StatusCode)"
            }
            else {
                $status = "LISTEN"
            }
        }
    }

    [PSCustomObject]@{
        Name = $target.Name
        Port = $target.Port
        PidFile = $pidValue
        ListeningPid = if ($listener) { $listener.OwningProcess } else { "" }
        Status = $status
        Url = $target.Url
    }
}

$rows | Format-Table -AutoSize
