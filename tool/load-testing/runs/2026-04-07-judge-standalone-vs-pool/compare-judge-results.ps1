param(
    [Parameter(Mandatory = $true)]
    [string]$StandaloneSummary,

    [Parameter(Mandatory = $true)]
    [string]$PoolSummary
)

function Get-MetricValue {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Summary,

        [Parameter(Mandatory = $true)]
        [string]$Metric,

        [Parameter(Mandatory = $true)]
        [string]$Field
    )

    return [double]$Summary.metrics.$Metric.$Field
}

function Format-PercentDelta {
    param(
        [double]$OldValue,
        [double]$NewValue,
        [bool]$HigherIsBetter = $false
    )

    if ($OldValue -eq 0) {
        return "n/a"
    }

    if ($HigherIsBetter) {
        $delta = (($NewValue - $OldValue) / $OldValue) * 100
    } else {
        $delta = (($OldValue - $NewValue) / $OldValue) * 100
    }

    return "{0:N2}%" -f $delta
}

function Get-CheckPassRate {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Summary,

        [Parameter(Mandatory = $true)]
        [string]$CheckName
    )

    $check = $Summary.root_group.checks.$CheckName
    if (-not $check) {
        return [double]::NaN
    }

    $passes = [double]$check.passes
    $fails = [double]$check.fails
    $total = $passes + $fails
    if ($total -le 0) {
        return [double]::NaN
    }

    return $passes / $total
}

$standalone = Get-Content $StandaloneSummary -Raw | ConvertFrom-Json
$pool = Get-Content $PoolSummary -Raw | ConvertFrom-Json

$standaloneAvg = Get-MetricValue -Summary $standalone -Metric "http_req_duration" -Field "avg"
$standaloneP95 = Get-MetricValue -Summary $standalone -Metric "http_req_duration" -Field "p(95)"
$standaloneP99 = Get-MetricValue -Summary $standalone -Metric "http_req_duration" -Field "p(99)"
$standaloneTps = Get-MetricValue -Summary $standalone -Metric "http_reqs" -Field "rate"
$standaloneErrorRate = Get-MetricValue -Summary $standalone -Metric "http_req_failed" -Field "value"
$standaloneBizPassRate = Get-CheckPassRate -Summary $standalone -CheckName "business code is 1000"

$poolAvg = Get-MetricValue -Summary $pool -Metric "http_req_duration" -Field "avg"
$poolP95 = Get-MetricValue -Summary $pool -Metric "http_req_duration" -Field "p(95)"
$poolP99 = Get-MetricValue -Summary $pool -Metric "http_req_duration" -Field "p(99)"
$poolTps = Get-MetricValue -Summary $pool -Metric "http_reqs" -Field "rate"
$poolErrorRate = Get-MetricValue -Summary $pool -Metric "http_req_failed" -Field "value"
$poolBizPassRate = Get-CheckPassRate -Summary $pool -CheckName "business code is 1000"

$rows = @(
    [PSCustomObject]@{
        Mode = "Standalone"
        AvgMs = "{0:N2}" -f $standaloneAvg
        P95Ms = "{0:N2}" -f $standaloneP95
        P99Ms = "{0:N2}" -f $standaloneP99
        TPS = "{0:N2}" -f $standaloneTps
        ErrorRate = "{0:P2}" -f $standaloneErrorRate
        BizSuccess = "{0:P2}" -f $standaloneBizPassRate
    }
    [PSCustomObject]@{
        Mode = "Pool"
        AvgMs = "{0:N2}" -f $poolAvg
        P95Ms = "{0:N2}" -f $poolP95
        P99Ms = "{0:N2}" -f $poolP99
        TPS = "{0:N2}" -f $poolTps
        ErrorRate = "{0:P2}" -f $poolErrorRate
        BizSuccess = "{0:P2}" -f $poolBizPassRate
    }
)

$rows | Format-Table -AutoSize

Write-Host ""
Write-Host "Improvement summary (Pool compared with Standalone):"
Write-Host ("  Avg latency improvement: {0}" -f (Format-PercentDelta -OldValue $standaloneAvg -NewValue $poolAvg))
Write-Host ("  P95 latency improvement: {0}" -f (Format-PercentDelta -OldValue $standaloneP95 -NewValue $poolP95))
Write-Host ("  P99 latency improvement: {0}" -f (Format-PercentDelta -OldValue $standaloneP99 -NewValue $poolP99))
Write-Host ("  Completed-task TPS improvement: {0}" -f (Format-PercentDelta -OldValue $standaloneTps -NewValue $poolTps -HigherIsBetter $true))
