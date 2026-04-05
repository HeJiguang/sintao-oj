$ErrorActionPreference = "Stop"

$workflowPath = ".github/workflows/deploy-notify.yml"

if (-not (Test-Path $workflowPath)) {
    throw "deploy-notify.yml is missing"
}

$content = Get-Content $workflowPath -Raw

$requiredSnippets = @(
    "workflow_run:",
    "workflows:",
    "- ci",
    "github.event.workflow_run.conclusion == 'success'",
    "github.event.workflow_run.head_branch == 'main'",
    "DEPLOY_NOTIFY_SMTP_HOST",
    "DEPLOY_NOTIFY_SMTP_PORT",
    "DEPLOY_NOTIFY_SMTP_USERNAME",
    "DEPLOY_NOTIFY_SMTP_PASSWORD",
    "DEPLOY_NOTIFY_TO",
    "workflows/cd.yml",
    "source_run_id",
    "This release will NOT auto-deploy."
)

foreach ($snippet in $requiredSnippets) {
    if (-not $content.Contains($snippet)) {
        throw "deploy-notify.yml is missing required content: $snippet"
    }
}
