$ErrorActionPreference = "Stop"

$workflowPath = ".github/workflows/cd.yml"
$content = Get-Content $workflowPath -Raw

if (-not $content.Contains("workflow_dispatch:")) {
    throw "cd.yml must allow manual workflow_dispatch deployments"
}

if ($content.Contains("workflow_run:")) {
    throw "cd.yml must not auto-trigger from workflow_run"
}

if ($content.Contains("github.event.workflow_run")) {
    throw "cd.yml still depends on workflow_run metadata"
}
