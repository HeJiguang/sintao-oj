$ErrorActionPreference = "Stop"

$workflowPaths = @(
    ".github/workflows/cd.yml",
    ".github/workflows/prod-recover.yml"
)

foreach ($path in $workflowPaths) {
    $content = Get-Content $path -Raw

    if (-not $content.Contains("/opt/syncode-runner/.ssh/onlineoj_worker_ci")) {
        throw "$path is missing the runner-local worker key path"
    }

    if (-not $content.Contains("RUNNER_WORKER_SSH_KEY_FILE")) {
        throw "$path is missing runner worker key file wiring"
    }
}
