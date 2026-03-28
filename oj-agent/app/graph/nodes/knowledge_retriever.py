from app.graph.nodes.node_utils import append_status, combined_text
from app.graph.state import AgentState
from app.retrieval import KeywordKnowledgeRetriever


def knowledge_retriever_node(state: AgentState) -> AgentState:
    retriever = KeywordKnowledgeRetriever()
    query = combined_text(
        state.get("question_title"),
        state.get("question_content"),
        state.get("judge_result"),
        state.get("user_message"),
    )
    hits = retriever.retrieve(query)
    if hits:
        message = "Retrieved knowledge snippets: " + ", ".join(hit.title for hit in hits)
    else:
        message = "No external knowledge snippet matched the current problem context."

    return {
        **state,
        "knowledge_hits": [
            {
                "source": hit.source,
                "title": hit.title,
                "excerpt": hit.excerpt,
                "score": hit.score,
            }
            for hit in hits
        ],
        "status_events": append_status(state, "knowledge_retriever", message),
    }
