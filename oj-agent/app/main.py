from fastapi import FastAPI

from app.api.debug import router as debug_router
from app.api.runs import router as runs_router
from app.api.training import router as training_router


app = FastAPI(title="OJ Agent", version="0.1.0")
app.include_router(runs_router)
app.include_router(debug_router)
app.include_router(training_router)


@app.get("/healthz")
def healthz() -> dict[str, str]:
    """本地联调和测试使用的最小健康检查接口。"""
    return {"status": "ok"}
