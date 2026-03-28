# OJ Agent

`oj-agent` is the standalone Python service that now acts as the formal AI microservice for OnlineOJ.

The active architecture is now:

```text
frontend -> gateway (/ai/chat*) -> oj-agent (/api/chat*)
oj-friend -> oj-agent (/api/training/plan)
oj-agent -> real LLM provider
```

`oj-ai` is no longer the intended mainline. The Python service is the main AI path.

The graph still provides intent routing and structured context, but final chat output and training planning can now use a real model provider.

## Runtime Modes

### Nacos-first mode (recommended)

Use Nacos as the single source of truth for the running service.

Recommended config file:

```text
deploy/dev/nacos/oj-agent-local-example.yaml
```

Publish it to Nacos as:

```text
dataId=oj-agent-local.yaml
group=DEFAULT_GROUP
namespace=c30aa94a-3644-48d8-9beb-20978bdd133b
```

Then start:

```powershell
./deploy/dev/agent/start-oj-agent-nacos.ps1
```

Bootstrap env used by the launcher:
- `OJ_AGENT_NACOS_SERVER_ADDR`
- `OJ_AGENT_NACOS_NAMESPACE`
- `OJ_AGENT_NACOS_GROUP`
- `OJ_AGENT_NACOS_CONFIG_DATA_ID`

The runtime config itself comes from Nacos.

### Local fallback mode

Copy:

```text
oj-agent/.env.local.example
```

to:

```text
oj-agent/.env.local
```

Then fill your real model values locally and start:

```powershell
./deploy/dev/agent/start-oj-agent-local.ps1
```

This mode is only for quick local fallback and is no longer the preferred mainline.

For the Java-side Nacos examples, use:
- [oj-gateway-local-agent-example.yaml](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/deploy/dev/nacos/oj-gateway-local-agent-example.yaml)
- [oj-friend-local-agent-example.yaml](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/deploy/dev/nacos/oj-friend-local-agent-example.yaml)
- [oj-agent-local-example.yaml](/D:/Project/OnlineOJ/bite-oj-master/bite-oj-master/deploy/dev/nacos/oj-agent-local-example.yaml)

## HTTP Surface

The Python service exposes:
- `POST /api/chat`
- `POST /api/chat/stream`
- `POST /api/training/plan`

The gateway-facing public AI path remains:
- `POST /ai/chat`
- `POST /ai/chat/stream`

## Example Request

```json
{
  "questionTitle": "Two Sum",
  "questionContent": "Find two numbers that add up to target.",
  "userCode": "public class Solution {}",
  "judgeResult": "WA on sample #2",
  "userMessage": "Why is this WA?"
}
```

Use the gateway-facing route for smoke tests when the Java stack is up.

## Current Behavior

- `oj-agent` accepts camelCase frontend payloads and gateway identity headers.
- `/api/chat/stream` emits `meta`, `status`, `delta`, and `final` SSE events.
- Training planning is `LLM-first with heuristic fallback`.
- The LangGraph flow now includes a retrieval step that can inject local OJ knowledge snippets into the assistant context.
- Optional Nacos registration is available through environment variables.

## Verification

Python:

```bash
pytest oj-agent/tests -q
```

Java:

```bash
mvn -q -pl oj-gateway,oj-modules/oj-friend -am -DskipTests compile
```
