param(
    [Parameter(Mandatory = $true)]
    [string[]]$Summaries
)

function Get-NullableNumber {
    param(
        $Value
    )
    if ($null -eq $Value -or [string]::IsNullOrWhiteSpace([string]$Value)) {
        return $null
    }
    return [double]$Value
}

function Get-PercentValue {
    param(
        [double]$Baseline,
        [double]$Candidate
    )
    if ($Baseline -le 0) {
        return $null
    }
    return [math]::Round((($Baseline - $Candidate) / $Baseline) * 100, 2)
}

$rows = foreach ($summaryPath in $Summaries) {
    $resolved = Resolve-Path $summaryPath -ErrorAction Stop
    $json = Get-Content $resolved -Raw | ConvertFrom-Json
    $ws = $json.summary.ws
    $poll = $json.summary.poll
    $comparison = $json.summary.comparison

    [PSCustomObject]@{
        Run = [IO.Path]::GetFileName($resolved)
        Samples = [int]$json.samples
        WsAvgMs = Get-NullableNumber $ws.avgMs
        PollAvgMs = Get-NullableNumber $poll.avgMs
        WsP95Ms = Get-NullableNumber $ws.p95Ms
        PollP95Ms = Get-NullableNumber $poll.p95Ms
        AvgImprovementPct = Get-PercentValue -Baseline (Get-NullableNumber $poll.avgMs) -Candidate (Get-NullableNumber $ws.avgMs)
        P95ImprovementPct = Get-PercentValue -Baseline (Get-NullableNumber $poll.p95Ms) -Candidate (Get-NullableNumber $ws.p95Ms)
        AvgDeltaMs = Get-NullableNumber $comparison.avgDeltaMs
        AvgPollRequests = Get-NullableNumber $poll.avgPollRequests
        WsSuccess = "{0}%" -f $ws.successRatePct
        PollSuccess = "{0}%" -f $poll.successRatePct
    }
}

$rows | Format-Table -AutoSize
