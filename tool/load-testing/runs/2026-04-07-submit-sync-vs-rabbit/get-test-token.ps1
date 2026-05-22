param(
    [string]$BaseUrl = "",
    [string]$Email = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir "config.ps1")

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = $DefaultBaseUrl
}
if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = "loadtest+$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())@example.com"
}

$sendBody = @{ email = $Email } | ConvertTo-Json -Compress
$sendResponse = Invoke-WebRequest -UseBasicParsing "$BaseUrl/friend/user/sendCode" `
    -Method POST -ContentType "application/json" -Body $sendBody
$sendJson = $sendResponse.Content | ConvertFrom-Json
if ($sendJson.code -ne 1000) {
    throw "sendCode failed: $($sendResponse.Content)"
}

$loginBody = @{ email = $Email; code = $DefaultJwtCode } | ConvertTo-Json -Compress
$loginResponse = Invoke-WebRequest -UseBasicParsing "$BaseUrl/friend/user/code/login" `
    -Method POST -ContentType "application/json" -Body $loginBody
$loginJson = $loginResponse.Content | ConvertFrom-Json
if ($loginJson.code -ne 1000 -or [string]::IsNullOrWhiteSpace([string]$loginJson.data)) {
    throw "codeLogin failed: $($loginResponse.Content)"
}

[PSCustomObject]@{
    email = $Email
    token = [string]$loginJson.data
} | ConvertTo-Json -Compress
