param(
    [string]$JdbcUrl = "",
    [string]$DbUser = "",
    [string]$DbPassword = ""
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir "config.ps1")

if ([string]::IsNullOrWhiteSpace($JdbcUrl)) {
    $JdbcUrl = if ($env:SPRING_DATASOURCE_URL) { $env:SPRING_DATASOURCE_URL } else { $DefaultJdbcUrl }
}
if ([string]::IsNullOrWhiteSpace($DbUser)) {
    $DbUser = if ($env:SPRING_DATASOURCE_USERNAME) { $env:SPRING_DATASOURCE_USERNAME } else { $DefaultJdbcUser }
}
if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    $DbPassword = if ($env:SPRING_DATASOURCE_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD } else { $DefaultJdbcPassword }
}

$connector = Get-ChildItem -Path (Join-Path $scriptDir "..\..\..\..\.m2-repo"), (Join-Path $HOME ".m2\repository") `
    -Recurse -Filter "mysql-connector-j-*.jar" -ErrorAction SilentlyContinue |
    Sort-Object FullName |
    Select-Object -First 1

if (-not $connector) {
    throw "MySQL connector jar not found"
}

$javaFile = Join-Path $scriptDir "SmokeQuestionSeeder.java"
$contentBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($SmokeQuestionContent))
$questionCaseBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($SmokeQuestionCaseJson))
$defaultCodeBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($SmokeQuestionDefaultCode))
$mainFucBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($SmokeQuestionMainFuc))
$esUri = if ($env:SPRING_ELASTICSEARCH_URIS) { $env:SPRING_ELASTICSEARCH_URIS } else { "http://127.0.0.1:9200" }

Write-Host "Seeding smoke question for submit benchmark..."
Write-Host "  JDBC URL: $JdbcUrl"
Write-Host "  JDBC User: $DbUser"
Write-Host "  Connector: $($connector.FullName)"
Write-Host "  Question ID: $SmokeQuestionId"

& java --class-path $connector.FullName $javaFile `
    --jdbc-url $JdbcUrl `
    --user $DbUser `
    --password $DbPassword `
    --question-id $SmokeQuestionId `
    --title $SmokeQuestionTitle `
    --difficulty $SmokeQuestionDifficulty `
    --time-limit $SmokeQuestionTimeLimit `
    --space-limit $SmokeQuestionSpaceLimit `
    --content-base64 $contentBase64 `
    --question-case-base64 $questionCaseBase64 `
    --default-code-base64 $defaultCodeBase64 `
    --main-fuc-base64 $mainFucBase64

if ($LASTEXITCODE -ne 0) {
    throw "Smoke question seeding failed with exit code $LASTEXITCODE"
}

$esBody = @{
    questionId = [long]$SmokeQuestionId
    title = $SmokeQuestionTitle
    difficulty = [int]$SmokeQuestionDifficulty
    algorithmTag = "math"
    knowledgeTags = "addition,temp"
    estimatedMinutes = 5
    trainingEnabled = 1
    timeLimit = [long]$SmokeQuestionTimeLimit
    spaceLimit = [long]$SmokeQuestionSpaceLimit
    content = $SmokeQuestionContent
    questionCase = $SmokeQuestionCaseJson
    mainFuc = $SmokeQuestionMainFuc
    defaultCode = $SmokeQuestionDefaultCode
    createTime = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json -Compress

Write-Host "Refreshing Elasticsearch document..."
Write-Host "  ES URI: $esUri"

$esResponse = Invoke-WebRequest -UseBasicParsing "$esUri/idx_question/_doc/$SmokeQuestionId" `
    -Method PUT -ContentType "application/json" -Body $esBody

Write-Host "  ES response: $($esResponse.Content)"
