"""Domain 层核心数据模型。

这个文件只做一件事：
定义 agent 系统内部“什么是一次 run、一次 event、一个 artifact”。

这里不要写业务流程，也不要写大模型调用逻辑。
它应该始终保持为“稳定的数据词典”，这样无论上层 graph 怎么演进，
这些模型都能作为整个系统共同理解的数据边界。
"""

from datetime import datetime, timezone
from typing import Any, Literal
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field, model_validator


RunType = Literal[
    "interactive_tutor",
    "interactive_diagnosis",
    "interactive_recommendation",
    "interactive_review",
]


def utc_now_iso() -> str:
    """统一生成 UTC ISO 时间字符串。

    这里单独抽出来，是为了让 run / event / artifact 在时间格式上完全一致，
    后面如果要切换时间序列存储或做审计检索，也更容易统一处理。
    """
    return datetime.now(timezone.utc).isoformat()


class CamelModel(BaseModel):
    """让接口模型同时兼容前端 camelCase 和 Python 内部 snake_case。

    前端请求体天然更偏向 `runType`、`questionId` 这种风格，
    但 Python 代码里我们仍然希望用 `run_type`、`question_id`。
    这个基类的目的就是把这种命名差异收敛到模型层解决，
    避免在业务代码里到处手写字段转换。
    """

    model_config = ConfigDict(populate_by_name=True)

    @model_validator(mode="before")
    @classmethod
    def normalize_camel_case(cls, data):
        """在模型真正校验前，把常见 camelCase 字段映射成 snake_case。"""
        if not isinstance(data, dict):
            return data
        normalized = dict(data)
        key_map = {
            "runType": "run_type",
            "questionId": "question_id",
            "questionTitle": "question_title",
            "questionContent": "question_content",
            "userCode": "user_code",
            "judgeResult": "judge_result",
            "userMessage": "user_message",
        }
        for source_key, target_key in key_map.items():
            if source_key in normalized and target_key not in normalized:
                normalized[target_key] = normalized[source_key]
        return normalized


class RunContext(CamelModel):
    """一次 agent 请求的工作区上下文。

    这里描述的是“用户当前手头有什么材料”：
    - 题目标题/题面
    - 当前代码
    - 最近一次判题反馈
    - 用户这次输入的问题

    它本质上是 agent 的输入现场，而不是数据库实体。
    """

    question_id: str | None = None
    question_title: str | None = None
    question_content: str | None = None
    user_code: str | None = None
    judge_result: str | None = None
    user_message: str


class CreateRunRequest(CamelModel):
    """HTTP `POST /api/runs` 的请求体模型。

    这个模型是 API 层和运行时之间的第一道边界：
    只要请求能被它成功解析，后面的 run service 就可以拿到结构稳定的输入。
    """

    run_type: RunType
    source: str
    context: RunContext


class RunRecord(BaseModel):
    """一次 agent 运行的主记录。

    可以把它理解成“线程头部信息”：
    - 这次 run 的唯一 ID
    - 是 tutor / diagnosis / recommendation / review 中的哪一种
    - 当前状态是什么
    - 它是由哪个 graph 负责执行的

    目前是内存态模型，后面即使接数据库，这个语义也应该保持不变。
    """

    run_id: str = Field(default_factory=lambda: f"run_{uuid4().hex}")
    run_type: RunType
    source: str
    status: str = "ACCEPTED"
    entry_graph: str = "oj_tutor_supervisor"
    created_at: str = Field(default_factory=utc_now_iso)


class RunEventRecord(BaseModel):
    """一次 run 过程中产生的离散事件。

    前端的 SSE 展示其实不是直接消费 graph 内部状态，
    而是消费这种事件流记录。它是“运行时内部过程”对外暴露的最小单位。
    """

    event_id: str = Field(default_factory=lambda: f"evt_{uuid4().hex}")
    run_id: str
    seq: int
    event_type: str
    payload: dict[str, Any] = Field(default_factory=dict)


class EvidenceItem(BaseModel):
    """最终回答里可展示的一条证据片段。

    注意它不是“所有 observation 的原始副本”，
    而是已经被整理成适合前端展示的证据卡片最小结构。
    """

    source_id: str
    title: str
    snippet: str


class ArtifactBody(BaseModel):
    """最终 artifact 的正文内容。

    当前前端真正关心的是：
    - `intent`：这次回答属于什么类型
    - `answer`：最终正文
    - `next_action`：下一步建议
    - `evidence`：证据列表

    所以 body 是真正的“用户可读核心内容”。
    """

    intent: str
    answer: str
    next_action: str
    evidence: list[EvidenceItem] = Field(default_factory=list)


class ArtifactRecord(BaseModel):
    """一次 run 最终沉淀出的结果卡片。

    你可以把它理解成前端最终渲染对象：
    title / summary 决定卡片头部，
    body 决定正文，
    render_hint 决定前端该按什么方式渲染。
    """

    artifact_id: str = Field(default_factory=lambda: f"art_{uuid4().hex}")
    run_id: str
    artifact_type: str = "answer_card"
    title: str
    summary: str
    body: ArtifactBody
    render_hint: str = "markdown"
    version: int = 1
    created_at: str = Field(default_factory=utc_now_iso)
