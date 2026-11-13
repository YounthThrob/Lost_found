import os
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.account.router import router as account_router
from app.bookmark.router import router as bookmark_router
from app.claim.router import router as claim_router
from app.clue.router import router as clue_router
from app.communication.router import router as communication_router
from app.config import STORAGE_LOCAL_DIR
from app.directory.router import router as directory_router
from app.handover.router import router as handover_router
from app.listing.router import router as listing_router
from app.moderation.router import router as moderation_router
from app.operations.router import router as operations_router
from app.shared.errors import register_exception_handlers


def _cors_origins() -> list[str]:
    # 仅允许配置的运营后台来源，禁止生产环境使用通配来源。
    raw = os.getenv("CORS_ORIGINS", "http://localhost:5173,http://127.0.0.1:5173")
    return [o.strip() for o in raw.split(",") if o.strip()]


app = FastAPI(title="校园失物招领互助平台 Python 后端", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
register_exception_handlers(app)

# 图片静态托管：/media/{storage_key} -> STORAGE_LOCAL_DIR/{storage_key}
_media_dir = Path(STORAGE_LOCAL_DIR).resolve()
_media_dir.mkdir(parents=True, exist_ok=True)
app.mount("/media", StaticFiles(directory=str(_media_dir)), name="media")
app.include_router(account_router, prefix="/api/v1")
app.include_router(directory_router, prefix="/api/v1")
app.include_router(bookmark_router, prefix="/api/v1")
app.include_router(listing_router, prefix="/api/v1")
app.include_router(moderation_router, prefix="/api/v1")
app.include_router(operations_router, prefix="/api/v1")
app.include_router(claim_router, prefix="/api/v1")
app.include_router(clue_router, prefix="/api/v1")
app.include_router(communication_router, prefix="/api/v1")
app.include_router(handover_router, prefix="/api/v1")
