def build_managed_context(*, run_type: str, source: str, request_context: dict, tool_catalog: list[dict]) -> dict:
    """构造图运行过程中统一使用的上下文对象。

    这里集中管理：
    - 原始请求上下文
    - 运行类型
    - 来源
    - 工具目录
    - 统一的教学约束
    """

    return {
        "run_type": run_type,
        "source": source,
        "request_context": {
            "question_id": request_context.get("question_id"),
            "question_title": request_context.get("question_title"),
            "question_content": request_context.get("question_content"),
            "user_code": request_context.get("user_code"),
            "judge_result": request_context.get("judge_result"),
            "user_message": request_context.get("user_message"),
        },
        "tool_catalog": tool_catalog,
        "learning_constraints": {
            "default_answer_style": "teaching_first",
            "full_solution_policy": "do_not_offer_direct_submission_by_default",
        },
    }
