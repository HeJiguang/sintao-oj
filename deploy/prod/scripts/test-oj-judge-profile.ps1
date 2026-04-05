$ErrorActionPreference = "Stop"

$stackPath = "deploy/prod/swarm/stack.yml"
$content = Get-Content $stackPath -Raw

if (-not $content.Contains("oj-judge:")) {
    throw "stack.yml is missing oj-judge service"
}

if (-not $content.Contains("SPRING_PROFILES_ACTIVE: swarm")) {
    throw "oj-judge service is missing swarm profile override"
}
