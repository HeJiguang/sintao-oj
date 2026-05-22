from app.runtime.retrieval_service import build_default_knowledge_retriever


class RequestBoundDataSources:
    """默认数据源实现。

    第一阶段先从当前请求上下文读取题目、代码和判题信息。
    这里不做任何决策，只负责把外部数据原样提供给工具层。
    """

    def __init__(self, knowledge_retriever=None) -> None:
        self.knowledge_retriever = (
            knowledge_retriever if knowledge_retriever is not None else build_default_knowledge_retriever()
        )

    def fetch_question_context(self, *, request_context: dict, arguments: dict) -> dict:
        return {
            "question_id": arguments.get("question_id", request_context.get("question_id")),
            "title": arguments.get("title", request_context.get("question_title")),
            "content": arguments.get("content", request_context.get("question_content")),
        }

    def fetch_workspace_snapshot(self, *, request_context: dict, arguments: dict) -> dict:
        return {
            "question_id": arguments.get("question_id", request_context.get("question_id")),
            "language": arguments.get("language", request_context.get("language", "java")),
            "user_code": arguments.get("user_code", request_context.get("user_code")),
        }

    def fetch_judge_snapshot(self, *, request_context: dict, arguments: dict) -> dict:
        return {
            "question_id": arguments.get("question_id", request_context.get("question_id")),
            "raw_result": arguments.get("judge_result", request_context.get("judge_result")),
        }

    def fetch_knowledge_evidence(self, *, request_context: dict, arguments: dict) -> dict:
        if self.knowledge_retriever is None:
            return {
                "question_title": arguments.get("question_title", request_context.get("question_title")),
                "query": arguments.get("query"),
                "items": [],
                "provider_status": "unconfigured",
            }

        try:
            items = self.knowledge_retriever.search(
                request_context=request_context,
                query=arguments.get("query"),
                question_title=arguments.get("question_title", request_context.get("question_title")),
            )
        except Exception as exc:
            return {
                "question_title": arguments.get("question_title", request_context.get("question_title")),
                "query": arguments.get("query"),
                "items": [],
                "provider_status": "error",
                "error": str(exc),
            }

        return {
            "question_title": arguments.get("question_title", request_context.get("question_title")),
            "query": arguments.get("query"),
            "items": items,
            "provider_status": "ok" if items else "empty",
        }

    def fetch_user_recent_activity(self, *, request_context: dict, arguments: dict) -> dict:
        return {
            "question_id": arguments.get("question_id", request_context.get("question_id")),
            "recent_submissions": [],
            "recent_focus_tags": [],
            "provider_status": "unconfigured",
        }
