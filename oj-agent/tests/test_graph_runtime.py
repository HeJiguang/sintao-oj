from pathlib import Path

from app.runtime.context_manager import build_managed_context
from app.runtime.data_sources import RequestBoundDataSources
from app.runtime.graph import build_runtime_graph
from app.runtime.tools import build_default_tool_registry

from conftest import FakeKnowledgeRetriever, FakeLLM


def build_state(run_type: str) -> dict:
    tool_registry = build_default_tool_registry(
        RequestBoundDataSources(
            knowledge_retriever=FakeKnowledgeRetriever(),
        )
    )
    return {
        "run_id": "run_test",
        "run_type": run_type,
        "status": "ACCEPTED",
        "managed_context": build_managed_context(
            run_type=run_type,
            source="workspace_panel",
            request_context={
                "question_id": "1001",
                "question_title": "Two Sum",
                "question_content": "Given an array of integers nums and an integer target...",
                "user_code": "class Solution {}",
                "judge_result": "WA on sample #2",
                "user_message": "Help me.",
            },
            tool_catalog=tool_registry.describe(),
        ),
        "observations": [],
        "projected_events": [],
        "projected_artifacts": [],
        "errors": [],
    }


def test_graph_builds_final_answer_for_tutor_run():
    graph = build_runtime_graph(llm=FakeLLM(), tool_registry=build_default_tool_registry())

    result = graph.invoke(build_state("interactive_tutor"))

    assert result["final_answer_draft"]["title"] == "解题提示"
    assert result["final_answer_draft"]["intent"] == "explain_problem"
    assert result["projected_events"]
    assert result["projected_artifacts"][-1]["body"]["intent"] == "explain_problem"


def test_graph_builds_diagnosis_answer_for_diagnosis_run():
    graph = build_runtime_graph(llm=FakeLLM(), tool_registry=build_default_tool_registry())

    result = graph.invoke(build_state("interactive_diagnosis"))

    assert result["final_answer_draft"]["title"] == "诊断总结"
    assert result["final_answer_draft"]["intent"] == "analyze_failure"
    assert result["projected_artifacts"][-1]["body"]["intent"] == "analyze_failure"


def test_graph_uses_question_and_workspace_tools_for_diagnosis():
    tool_registry = build_default_tool_registry(
        RequestBoundDataSources(
            knowledge_retriever=FakeKnowledgeRetriever(),
        )
    )
    graph = build_runtime_graph(llm=FakeLLM(), tool_registry=tool_registry)

    result = graph.invoke(build_state("interactive_diagnosis"))

    tool_names = [item["tool_name"] for item in result["observations"]]
    assert "get_question_context" in tool_names
    assert "get_workspace_snapshot" in tool_names
    assert "get_judge_snapshot" in tool_names
    assert "retrieve_knowledge_evidence" in tool_names


def test_runtime_nodes_are_split_into_one_file_per_graph_node():
    runtime_dir = Path(__file__).resolve().parents[1] / "app" / "runtime"
    nodes_dir = runtime_dir / "nodes"

    assert nodes_dir.is_dir()
    assert not (runtime_dir / "brains.py").exists()
    assert not (runtime_dir / "prompts.py").exists()

    expected_files = {
        "__init__.py",
        "context_node.py",
        "planner_node.py",
        "tool_node.py",
        "responder_node.py",
    }

    actual_files = {item.name for item in nodes_dir.iterdir() if item.is_file()}
    assert expected_files.issubset(actual_files)


def test_graph_loops_from_planner_to_tool_until_final_response():
    graph = build_runtime_graph(llm=FakeLLM(), tool_registry=build_default_tool_registry())

    result = graph.invoke(build_state("interactive_diagnosis"))

    assert result["action"]["type"] == "final"
    assert len(result["observations"]) == 4


def test_planner_tracks_rounds_and_action_history_across_loop():
    graph = build_runtime_graph(llm=FakeLLM(), tool_registry=build_default_tool_registry())

    result = graph.invoke(build_state("interactive_diagnosis"))

    assert result["planner_round"] == 5
    assert [item["type"] for item in result["planner_trace"]] == ["tool", "tool", "tool", "tool", "final"]
    assert [item.get("tool") for item in result["planner_trace"][:3]] == [
        "get_question_context",
        "get_workspace_snapshot",
        "get_judge_snapshot",
    ]


def test_graph_projects_retrieved_knowledge_into_artifact_evidence():
    graph = build_runtime_graph(
        llm=FakeLLM(),
        tool_registry=build_default_tool_registry(
            RequestBoundDataSources(
                knowledge_retriever=FakeKnowledgeRetriever(),
            )
        ),
    )

    result = graph.invoke(build_state("interactive_diagnosis"))

    evidence = result["projected_artifacts"][-1]["body"]["evidence"]
    source_ids = {item["source_id"] for item in evidence}

    assert "knowledge_hash_map" in source_ids
