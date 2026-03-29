from app.guardrails.runtime import GuardrailInput, GuardrailRuntime
from app.runtime.enums import RiskLevel


def test_guardrail_runtime_marks_missing_context_as_medium_risk():
    runtime = GuardrailRuntime()

    result = runtime.evaluate(
        GuardrailInput(
            task_type="chat",
            user_message="Help me look.",
            question_title=None,
            question_content=None,
            user_code=None,
            judge_result=None,
        )
    )

    assert result.risk_level is RiskLevel.MEDIUM
    assert result.completeness_ok is False
    assert "question statement" in result.missing_fields
    assert "code" in result.missing_fields
    assert "judge result" in result.missing_fields


def test_guardrail_runtime_blocks_direct_ac_code_policy():
    runtime = GuardrailRuntime()

    result = runtime.evaluate_output(
        answer="Here is the full AC code you can submit directly.",
        evidence_count=2,
    )

    assert result.policy_ok is False
    assert result.risk_level is RiskLevel.HIGH
    assert result.risk_reasons
