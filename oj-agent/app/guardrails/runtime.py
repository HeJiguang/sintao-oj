from pydantic import BaseModel, Field

from app.runtime.enums import RiskLevel


class GuardrailInput(BaseModel):
    task_type: str
    user_message: str
    question_title: str | None = None
    question_content: str | None = None
    user_code: str | None = None
    judge_result: str | None = None


class GuardrailOutput(BaseModel):
    risk_level: RiskLevel
    completeness_ok: bool
    policy_ok: bool
    missing_fields: list[str] = Field(default_factory=list)
    risk_reasons: list[str] = Field(default_factory=list)


class GuardrailRuntime:
    def evaluate(self, guardrail_input: GuardrailInput) -> GuardrailOutput:
        missing_fields: list[str] = []
        if not guardrail_input.question_title and not guardrail_input.question_content:
            missing_fields.append("question statement")
        if not guardrail_input.user_code:
            missing_fields.append("code")
        if not guardrail_input.judge_result:
            missing_fields.append("judge result")

        return GuardrailOutput(
            risk_level=RiskLevel.MEDIUM if missing_fields else RiskLevel.LOW,
            completeness_ok=not missing_fields,
            policy_ok=True,
            missing_fields=missing_fields,
            risk_reasons=["missing critical context"] if missing_fields else [],
        )

    def evaluate_output(self, *, answer: str, evidence_count: int) -> GuardrailOutput:
        normalized_answer = answer.lower()
        direct_code_leak = "full ac code" in normalized_answer or "submit directly" in normalized_answer
        unsupported_answer = evidence_count <= 0 and bool(answer.strip())

        risk_level = RiskLevel.LOW
        policy_ok = True
        risk_reasons: list[str] = []

        if direct_code_leak:
            risk_level = RiskLevel.HIGH
            policy_ok = False
            risk_reasons.append("direct solution leakage")
        elif unsupported_answer:
            risk_level = RiskLevel.MEDIUM
            risk_reasons.append("answer lacks evidence support")

        return GuardrailOutput(
            risk_level=risk_level,
            completeness_ok=True,
            policy_ok=policy_ok,
            missing_fields=[],
            risk_reasons=risk_reasons,
        )
