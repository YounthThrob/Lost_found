from fastapi import FastAPI

from app.account.router import router as account_router
from app.claim.router import router as claim_router
from app.communication.router import router as communication_router
from app.listing.router import router as listing_router
from app.moderation.router import router as moderation_router
from app.shared.errors import register_exception_handlers

app = FastAPI(title="校园失物招领互助平台 Python 后端", version="0.1.0")
register_exception_handlers(app)
app.include_router(account_router, prefix="/api/v1")
app.include_router(listing_router, prefix="/api/v1")
app.include_router(moderation_router, prefix="/api/v1")
app.include_router(claim_router, prefix="/api/v1")
app.include_router(communication_router, prefix="/api/v1")
