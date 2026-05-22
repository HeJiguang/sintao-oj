# OJ Agent

Workspace interactive tutor agent for OnlineOJ.

## Stage-One Retrieval

The current mainline keeps the four-node LangGraph runtime and plugs external evidence retrieval into the tool layer.

- `context_node` identifies missing evidence
- `planner_node` decides when to call retrieval
- `tool_node` executes `retrieve_knowledge_evidence`
- `responder_node` turns evidence into final artifacts

The first stage uses Qdrant as the vector store with a small seeded knowledge corpus.
