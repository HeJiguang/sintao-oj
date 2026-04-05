# GitHub Secrets Guide

This repository now uses a split pipeline:

- `ci.yml` runs on GitHub-hosted runners.
- `cd.yml` runs on the self-hosted runner installed on `101.96.200.76`.

The production deploy path no longer depends on GHCR or on GitHub-hosted runners SSHing into the manager for every release.

## Required Secrets

### `DEPLOY_SSH_HOST`

Used only by `bootstrap-runner.yml`.

Recommended value:

```text
101.96.200.76
```

### `DEPLOY_SSH_USER`

Used only by `bootstrap-runner.yml`.

Recommended value:

```text
root
```

### `DEPLOY_SSH_KEY`

Used only by `bootstrap-runner.yml`.

This must be the private key that can SSH into `101.96.200.76` as `DEPLOY_SSH_USER`.

### `SELF_HOSTED_RUNNER_BOOTSTRAP_TOKEN`

Used only by `bootstrap-runner.yml`.

This is a temporary GitHub runner registration token for this repository.

### `WORKER_SSH_KEY`

Used by `cd.yml`.

This must be a private key that allows the manager-hosted runner job to copy images to `101.96.200.77`.

If manager and worker already trust the same deploy key, reuse that key here.

### `STACK_ENV_PROD`

Used by `cd.yml`.

Copy the full content of [stack.env.prod.template](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/deploy/prod/env/stack.env.prod.template) into this secret, then replace:

- `PUBLIC_DOMAIN`
- `WORKER_CONSTRAINT` if the worker hostname changes
- `WORKER_SSH_HOST`, `WORKER_SSH_USER`, `WORKER_SSH_PORT` if needed
- `WORKER_IMAGE_LIST` if more services move onto the worker
- `JUDGE_HOST_USER_CODE_DIR` and `JUDGE_HOST_USER_CODE_POOL_DIR` if you want different host paths for judge sandbox files

Do not manually replace `sha-REPLACE_ME`. The workflow does that for each release.

### `RUNTIME_ENV_PROD`

Used by `cd.yml`.

Copy the full content of [runtime.env.prod.template](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/deploy/prod/env/runtime.env.prod.template) into this secret and replace the runtime placeholders with real production values.

This file should stay minimal. The Java business passwords and datasource details should continue to live in Nacos, not be duplicated into GitHub.

Keep these runtime bootstrap values:

- `NACOS_SERVER_ADDR`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`
- `NACOS_NAMESPACE`
- `NACOS_GROUP`
- `OJ_AGENT_NACOS_*`

Keep these judge-related values unless you have a deliberate alternative:

- `SANDBOX_DOCKER_HOST=unix:///var/run/docker.sock`
- `SANDBOX_DOCKER_VOLUME=/usr/share/java`

## Recommended Setup Order

1. Prepare `STACK_ENV_PROD`.
2. Prepare `RUNTIME_ENV_PROD`.
3. Verify SSH key access from manager to worker.
4. Generate a fresh `SELF_HOSTED_RUNNER_BOOTSTRAP_TOKEN`.
5. Run `bootstrap-runner.yml`.
6. Wait for the `syncode-prod` runner to show up in GitHub.
7. Trigger `cd.yml` manually once.
8. After the first successful release, let `workflow_run` auto-deploy from `main`.
