from typing import Literal

from pydantic import BaseModel, Field, model_validator


class ContextEnvelope(BaseModel):
    user_goal: str = Field(description="The concrete outcome the user wants from this run.")
    inferred_subtask: str = Field(description="The narrower subtask inferred from the current context.")
    wants_full_solution: bool = Field(default=False, description="Whether the user explicitly wants a full solution.")
    wants_partial_code: bool = Field(default=False, description="Whether the user wants partial code or code edits.")
    confidence: float = Field(default=0.0, description="Confidence for the current context understanding, in [0, 1].")
    salient_context: list[str] = Field(default_factory=list, description="The most important context facts for later nodes.")
    missing_context: list[str] = Field(default_factory=list, description="Useful context that is still missing.")


class PlannerAction(BaseModel):
    type: Literal["tool", "final"] = Field(
        description="Next action type. tool means invoke one tool, final means proceed to the final answer."
    )
    tool: str | None = Field(
        default=None,
        description="Tool name when type=tool. Must be empty when type=final.",
    )
    input: dict = Field(
        default_factory=dict,
        description="Structured tool input. Must be an empty object when type=final.",
    )
    reason: str = Field(description="Short reason for selecting the current action.")

    @model_validator(mode="after")
    def validate_tool_requirements(self):
        if self.type == "tool" and not self.tool:
            raise ValueError("tool is required when type=tool")
        if self.type == "final":
            self.tool = None
            self.input = {}
        return self


class FinalAnswer(BaseModel):
    intent: str = Field(description="Primary answer intent, for example explain_problem or analyze_failure.")
    title: str = Field(description="Short card title for the frontend.")
    summary: str = Field(description="One-line summary for the frontend card.")
    answer: str = Field(description="Full answer body shown to the user.")
    next_action: str = Field(description="The next concrete step the user should try.")
    evidence_refs: list[str] = Field(
        default_factory=list,
        description="Evidence ids referenced by this answer. They must map back to projected observations.",
    )
    confidence: float = Field(default=0.0, description="Overall answer confidence in [0, 1].")

    @model_validator(mode="after")
    def validate_text_fields(self):
        for field_name in ("intent", "title", "summary", "answer", "next_action"):
            if not getattr(self, field_name).strip():
                raise ValueError(f"{field_name} cannot be blank")
        return self


class FinalAnswerMeta(BaseModel):
    intent: str = Field(description="Primary answer intent, for example explain_problem or analyze_failure.")
    title: str = Field(description="Short card title for the frontend.")
    summary: str = Field(description="One-line summary for the frontend card.")
    next_action: str = Field(description="The next concrete step the user should try.")
    evidence_refs: list[str] = Field(
        default_factory=list,
        description="Evidence ids referenced by this answer. They must map back to projected observations.",
    )
    confidence: float = Field(default=0.0, description="Overall answer confidence in [0, 1].")

    @model_validator(mode="after")
    def validate_text_fields(self):
        for field_name in ("intent", "title", "summary", "next_action"):
            if not getattr(self, field_name).strip():
                raise ValueError(f"{field_name} cannot be blank")
        return self
