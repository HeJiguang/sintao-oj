from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.chat import router as chat_router
from app.api.training import router as training_router
from app.core.config import load_settings
from app.core.nacos_registry import NacosRegistry


@asynccontextmanager
async def lifespan(_app: FastAPI):
    settings = load_settings()
    registry = NacosRegistry(settings)
    try:
        registry.register()
    except Exception:
        # Keep oj-agent runnable even when Nacos is absent.
        pass

    try:
        yield
    finally:
        try:
            registry.deregister()
        except Exception:
            pass


app = FastAPI(title="OJ Agent", version="0.1.0", lifespan=lifespan)
app.include_router(chat_router)
app.include_router(training_router)
