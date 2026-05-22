from collections.abc import Callable

from langchain_core.tools import StructuredTool
from pydantic import BaseModel, Field

from app.runtime.data_sources import RequestBoundDataSources


ToolInputBuilder = Callable[[dict, dict], dict]


class ToolRegistry:
    """维护当前 agent 可调用的只读工具集合。"""

    def __init__(self, tools: dict[str, StructuredTool], input_builders: dict[str, ToolInputBuilder]) -> None:
        self._tools = tools
        self._input_builders = input_builders

    def invoke(self, tool_name: str, managed_context: dict, arguments: dict | None = None) -> dict:
        if tool_name not in self._tools:
            return {
                "tool_name": tool_name,
                "ok": False,
                "payload": {},
                "error": f"未知工具: {tool_name}",
            }

        tool = self._tools[tool_name]
        tool_input = self._input_builders[tool_name](managed_context, arguments or {})
        try:
            payload = tool.invoke(tool_input)
            return {
                "tool_name": tool_name,
                "ok": True,
                "payload": payload,
                "error": None,
            }
        except Exception as exc:
            return {
                "tool_name": tool_name,
                "ok": False,
                "payload": {"tool_input": tool_input},
                "error": str(exc),
            }

    def get(self, tool_name: str) -> StructuredTool:
        return self._tools[tool_name]

    def describe(self) -> list[dict]:
        """返回 planner 可见的工具目录。"""
        catalog: list[dict] = []
        for name, tool in self._tools.items():
            catalog.append(
                {
                    "name": name,
                    "description": tool.description,
                    "input_schema": tool.args_schema.model_json_schema() if tool.args_schema else {},
                }
            )
        return catalog


def build_default_tool_registry(data_sources: RequestBoundDataSources | None = None) -> ToolRegistry:
    """构造当前默认的工具注册表。"""
    sources = data_sources or RequestBoundDataSources()
    return ToolRegistry(
        tools={
            "get_question_context": _build_question_context_tool(sources),
            "get_workspace_snapshot": _build_workspace_snapshot_tool(sources),
            "get_judge_snapshot": _build_judge_snapshot_tool(sources),
            "retrieve_knowledge_evidence": _build_knowledge_evidence_tool(sources),
            "get_user_recent_activity": _build_user_recent_activity_tool(sources),
        },
        input_builders={
            "get_question_context": _build_question_context_input,
            "get_workspace_snapshot": _build_workspace_snapshot_input,
            "get_judge_snapshot": _build_judge_snapshot_input,
            "retrieve_knowledge_evidence": _build_knowledge_evidence_input,
            "get_user_recent_activity": _build_user_recent_activity_input,
        },
    )


class QuestionContextInput(BaseModel):
    question_id: str | None = Field(default=None, description="当前题目的唯一标识，如果已有就优先传入。")
    title: str | None = Field(default=None, description="题目标题，用于直接补足或覆盖题目上下文。")
    content: str | None = Field(default=None, description="题目正文，用于直接补足或覆盖题目题面内容。")


class WorkspaceSnapshotInput(BaseModel):
    question_id: str | None = Field(default=None, description="当前工作区对应的题目标识。")
    language: str = Field(default="java", description="当前代码所使用的编程语言。")
    user_code: str | None = Field(default=None, description="当前工作区中的代码内容。")


class JudgeSnapshotInput(BaseModel):
    question_id: str | None = Field(default=None, description="当前工作区对应的题目标识。")
    judge_result: str | None = Field(default=None, description="最近一次判题结果的原始文本。")


class KnowledgeEvidenceInput(BaseModel):
    question_title: str | None = Field(default=None, description="当前题目的标题，用于约束知识检索范围。")
    query: str | None = Field(default=None, description="planner 生成的检索查询语句。")


class UserRecentActivityInput(BaseModel):
    question_id: str | None = Field(default=None, description="当前题目标识，用于查询与当前题目相关的近期用户活动。")


def _build_question_context_tool(data_sources: RequestBoundDataSources) -> StructuredTool:
    def get_question_context(question_id: str | None = None, title: str | None = None, content: str | None = None) -> dict:
        return data_sources.fetch_question_context(
            request_context={},
            arguments={"question_id": question_id, "title": title, "content": content},
        )

    return StructuredTool.from_function(
        func=get_question_context,
        name="get_question_context",
        description="读取当前题目的标题和题面。",
        args_schema=QuestionContextInput,
    )


def _build_question_context_input(managed_context: dict, arguments: dict) -> dict:
    request_context = managed_context["request_context"]
    return {
        "question_id": arguments.get("question_id", request_context.get("question_id")),
        "title": arguments.get("title", request_context.get("question_title")),
        "content": arguments.get("content", request_context.get("question_content")),
    }


def _build_workspace_snapshot_tool(data_sources: RequestBoundDataSources) -> StructuredTool:
    def get_workspace_snapshot(
        question_id: str | None = None,
        language: str = "java",
        user_code: str | None = None,
    ) -> dict:
        return data_sources.fetch_workspace_snapshot(
            request_context={},
            arguments={"question_id": question_id, "language": language, "user_code": user_code},
        )

    return StructuredTool.from_function(
        func=get_workspace_snapshot,
        name="get_workspace_snapshot",
        description="读取当前工作区里的代码快照。",
        args_schema=WorkspaceSnapshotInput,
    )


def _build_workspace_snapshot_input(managed_context: dict, arguments: dict) -> dict:
    request_context = managed_context["request_context"]
    return {
        "question_id": arguments.get("question_id", request_context.get("question_id")),
        "language": arguments.get("language", request_context.get("language", "java")),
        "user_code": arguments.get("user_code", request_context.get("user_code")),
    }


def _build_judge_snapshot_tool(data_sources: RequestBoundDataSources) -> StructuredTool:
    def get_judge_snapshot(question_id: str | None = None, judge_result: str | None = None) -> dict:
        return data_sources.fetch_judge_snapshot(
            request_context={},
            arguments={"question_id": question_id, "judge_result": judge_result},
        )

    return StructuredTool.from_function(
        func=get_judge_snapshot,
        name="get_judge_snapshot",
        description="读取最近一次判题结果原文。",
        args_schema=JudgeSnapshotInput,
    )


def _build_judge_snapshot_input(managed_context: dict, arguments: dict) -> dict:
    request_context = managed_context["request_context"]
    return {
        "question_id": arguments.get("question_id", request_context.get("question_id")),
        "judge_result": arguments.get("judge_result", request_context.get("judge_result")),
    }


def _build_knowledge_evidence_tool(data_sources: RequestBoundDataSources) -> StructuredTool:
    def retrieve_knowledge_evidence(question_title: str | None = None, query: str | None = None) -> dict:
        return data_sources.fetch_knowledge_evidence(
            request_context={},
            arguments={"question_title": question_title, "query": query},
        )

    return StructuredTool.from_function(
        func=retrieve_knowledge_evidence,
        name="retrieve_knowledge_evidence",
        description="从知识检索系统读取与当前问题有关的证据。",
        args_schema=KnowledgeEvidenceInput,
    )


def _build_knowledge_evidence_input(managed_context: dict, arguments: dict) -> dict:
    request_context = managed_context["request_context"]
    return {
        "question_title": arguments.get("question_title", request_context.get("question_title")),
        "query": arguments.get("query"),
    }


def _build_user_recent_activity_tool(data_sources: RequestBoundDataSources) -> StructuredTool:
    def get_user_recent_activity(question_id: str | None = None) -> dict:
        return data_sources.fetch_user_recent_activity(
            request_context={},
            arguments={"question_id": question_id},
        )

    return StructuredTool.from_function(
        func=get_user_recent_activity,
        name="get_user_recent_activity",
        description="读取用户近期活动数据。",
        args_schema=UserRecentActivityInput,
    )


def _build_user_recent_activity_input(managed_context: dict, arguments: dict) -> dict:
    request_context = managed_context["request_context"]
    return {
        "question_id": arguments.get("question_id", request_context.get("question_id")),
    }
