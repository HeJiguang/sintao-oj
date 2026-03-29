from enum import Enum


class TaskType(str, Enum):
    CHAT = "chat"
    TRAINING_PLAN = "training_plan"
    REVIEW = "review"
    PROFILE = "profile"
    RECOMMENDATION = "recommendation"
    DIAGNOSIS = "diagnosis"


class RunStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    FAILED = "failed"
    DEGRADED = "degraded"


class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
