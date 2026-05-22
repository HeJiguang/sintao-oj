import json
import os
import sys
from pathlib import Path

import pytest


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


os.environ["OJ_AGENT_LLM_PROVIDER"] = "deepseek"
os.environ["OJ_AGENT_LLM_API_KEY"] = "test-key"
os.environ["OJ_AGENT_LLM_MODEL"] = "deepseek-chat"
os.environ["OJ_AGENT_LLM_BASE_URL"] = "https://api.deepseek.com"


loaded_app = sys.modules.get("app")
if loaded_app is not None:
    module_file = Path(getattr(loaded_app, "__file__", ""))
    if PROJECT_ROOT not in module_file.parents:
        sys.modules.pop("app", None)


from app.runtime.contracts import ContextEnvelope, FinalAnswer, PlannerAction
from app.runtime.data_sources import RequestBoundDataSources
from app.runtime.graph import build_runtime_graph
from app.runtime.tools import build_default_tool_registry
from app.services.run_service import run_service


class FakeKnowledgeRetriever:
    def search(self, *, request_context: dict, query: str | None, question_title: str | None, top_k: int = 3) -> list[dict]:
        del request_context, top_k
        return [
            {
                "source_id": "knowledge_hash_map",
                "title": "Hash Map Complement Pattern",
                "snippet": f"Use complement lookup before insert for {question_title or query}.",
                "source_name": "seed",
                "source_url": "https://example.test/hash-map",
                "license": "internal-seed",
                "score": 0.95,
            }
        ]


class FakeLLM:
    """Deterministic fake model used by runtime tests."""

    def invoke_structured(self, *, schema, system_prompt: str, user_prompt: str) -> dict:
        del system_prompt
        payload = json.loads(user_prompt.splitlines()[-1])
        managed_context = payload["managed_context"]
        request_context = managed_context["request_context"]
        schema_name = getattr(schema, "__name__", "")

        if schema is ContextEnvelope:
            return {
                "user_goal": "diagnose current failure"
                if managed_context["run_type"] == "interactive_diagnosis"
                else "give a hint",
                "inferred_subtask": managed_context["run_type"],
                "wants_full_solution": False,
                "wants_partial_code": managed_context["run_type"] == "interactive_diagnosis",
                "confidence": 0.9,
                "salient_context": [request_context["question_title"]],
                "missing_context": [],
            }

        if schema is PlannerAction:
            observed_tools = {item["tool_name"] for item in payload.get("observations", [])}
            if managed_context["run_type"] == "interactive_diagnosis":
                if "get_question_context" not in observed_tools:
                    return {
                        "type": "tool",
                        "tool": "get_question_context",
                        "input": {},
                        "reason": "Need the problem statement",
                    }
                if "get_workspace_snapshot" not in observed_tools:
                    return {
                        "type": "tool",
                        "tool": "get_workspace_snapshot",
                        "input": {},
                        "reason": "Need the current code",
                    }
                if "get_judge_snapshot" not in observed_tools:
                    return {
                        "type": "tool",
                        "tool": "get_judge_snapshot",
                        "input": {},
                        "reason": "Need the latest judge result",
                    }
                if "retrieve_knowledge_evidence" not in observed_tools:
                    return {
                        "type": "tool",
                        "tool": "retrieve_knowledge_evidence",
                        "input": {"query": "hash map complement duplicate values"},
                        "reason": "Need external algorithm evidence",
                    }
                return {"type": "final", "reason": "Enough diagnosis evidence"}

            if "get_question_context" not in observed_tools:
                return {
                    "type": "tool",
                    "tool": "get_question_context",
                    "input": {},
                    "reason": "Need the problem statement",
                }
            return {"type": "final", "reason": "Enough tutor context"}

        if schema is FinalAnswer and managed_context["run_type"] == "interactive_diagnosis":
            return {
                "title": "诊断总结",
                "summary": "重复值场景下的哈希表更新顺序不对。",
                "answer": "你的主要问题不是题意，而是写入哈希表的时机过早，导致重复值场景判断失效。",
                "next_action": "先手推 [3,3]，确认是先查 complement 还是先写入当前值。",
                "evidence_refs": ["question_context", "workspace_snapshot", "judge_snapshot", "knowledge_1"],
                "confidence": 0.88,
                "intent": "analyze_failure",
            }

        if schema is FinalAnswer:
            return {
                "title": "解题提示",
                "summary": "先从补数查找的思路入手。",
                "answer": "不要先想完整代码，先思考当前数字需要找的补数，以及如何快速判断补数是否出现过。",
                "next_action": "先手推样例 [2,7,11,15], 9。",
                "evidence_refs": ["question_context"],
                "confidence": 0.84,
                "intent": "explain_problem",
            }

        if schema_name == "FinalAnswerMeta" and managed_context["run_type"] == "interactive_diagnosis":
            return {
                "title": "诊断总结",
                "summary": "重复值场景下的哈希表更新顺序不对。",
                "next_action": "先手推 [3,3]，确认是先查 complement 还是先写入当前值。",
                "evidence_refs": ["question_context", "workspace_snapshot", "judge_snapshot", "knowledge_1"],
                "confidence": 0.88,
                "intent": "analyze_failure",
            }

        if schema_name == "FinalAnswerMeta":
            return {
                "title": "解题提示",
                "summary": "先从补数查找的思路入手。",
                "next_action": "先手推样例 [2,7,11,15], 9。",
                "evidence_refs": ["question_context"],
                "confidence": 0.84,
                "intent": "explain_problem",
            }

        raise AssertionError(f"Unhandled test schema: {schema}")

    def stream_text(self, *, system_prompt: str, user_prompt: str):
        del system_prompt
        payload = json.loads(user_prompt.splitlines()[-1])
        managed_context = payload["managed_context"]

        if managed_context["run_type"] == "interactive_diagnosis":
            yield "你的主要问题不是题意，"
            yield "而是写入哈希表的时机过早，"
            yield "导致重复值场景判断失效。"
            return

        yield "不要先想完整代码，"
        yield "先思考当前数字需要找的补数，"
        yield "以及如何快速判断补数是否出现过。"


@pytest.fixture(autouse=True)
def patch_runtime_graph_for_tests():
    original_graph = run_service.runtime_graph
    original_tool_registry = run_service.tool_registry
    test_registry = build_default_tool_registry(
        RequestBoundDataSources(
            knowledge_retriever=FakeKnowledgeRetriever(),
        )
    )
    run_service.tool_registry = test_registry
    run_service.runtime_graph = build_runtime_graph(
        llm=FakeLLM(),
        tool_registry=test_registry,
    )
    run_service.clear()
    yield
    run_service.runtime_graph = original_graph
    run_service.tool_registry = original_tool_registry
    run_service.clear()
