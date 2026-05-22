param(
    [Parameter(Mandatory = $true)]
    [string]$SyncSummary,

    [Parameter(Mandatory = $true)]
    [string]$AsyncSummary
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

$sync = Get-Content $SyncSummary -Raw | ConvertFrom-Json
$async = Get-Content $AsyncSummary -Raw | ConvertFrom-Json

$syncAvg = Get-MetricValue -Summary $sync -Metric "http_req_duration" -Field "avg"
$syncP95 = Get-MetricValue -Summary $sync -Metric "http_req_duration" -Field "p(95)"
$syncP99 = Get-MetricValue -Summary $sync -Metric "http_req_duration" -Field "p(99)"
$syncRps = Get-MetricValue -Summary $sync -Metric "http_reqs" -Field "rate"
$syncErrorRate = Get-MetricValue -Summary $sync -Metric "http_req_failed" -Field "value"
$syncBizPassRate = Get-CheckPassRate -Summary $sync -CheckName "business code is 1000"

$asyncAvg = Get-MetricValue -Summary $async -Metric "http_req_duration" -Field "avg"
$asyncP95 = Get-MetricValue -Summary $async -Metric "http_req_duration" -Field "p(95)"
$asyncP99 = Get-MetricValue -Summary $async -Metric "http_req_duration" -Field "p(99)"
$asyncRps = Get-MetricValue -Summary $async -Metric "http_reqs" -Field "rate"
$asyncErrorRate = Get-MetricValue -Summary $async -Metric "http_req_failed" -Field "value"
$asyncBizPassRate = Get-CheckPassRate -Summary $async -CheckName "business code is 1000"

$rows = @(
    [PSCustomObject]@{
        Interface = "Sync submit"
        AvgMs = "{0:N2}" -f $syncAvg
        P95Ms = "{0:N2}" -f $syncP95
        P99Ms = "{0:N2}" -f $syncP99
        ReqPerSec = "{0:N2}" -f $syncRps
        ErrorRate = "{0:P2}" -f $syncErrorRate
        BizSuccess = "{0:P2}" -f $syncBizPassRate
    }
    [PSCustomObject]@{
        Interface = "Async submit"
        AvgMs = "{0:N2}" -f $asyncAvg
        P95Ms = "{0:N2}" -f $asyncP95
        P99Ms = "{0:N2}" -f $asyncP99
        ReqPerSec = "{0:N2}" -f $asyncRps
        ErrorRate = "{0:P2}" -f $asyncErrorRate
        BizSuccess = "{0:P2}" -f $asyncBizPassRate
    }
)

$rows | Format-Table -AutoSize

Write-Host ""
Write-Host "Improvement summary (Async compared with Sync):"
Write-Host ("  Avg latency improvement: {0}" -f (Format-PercentDelta -OldValue $syncAvg -NewValue $asyncAvg))
Write-Host ("  P95 latency improvement: {0}" -f (Format-PercentDelta -OldValue $syncP95 -NewValue $asyncP95))
Write-Host ("  P99 latency improvement: {0}" -f (Format-PercentDelta -OldValue $syncP99 -NewValue $asyncP99))
Write-Host ("  Throughput improvement: {0}" -f (Format-PercentDelta -OldValue $syncRps -NewValue $asyncRps -HigherIsBetter $true))
