from fastapi import FastAPI

from app.account.router import router as account_router
from app.shared.errors import register_exception_handlers

app = FastAPI(title="校园失物招领互助平台 Python 后端", version="0.1.0")
register_exception_handlers(app)
app.include_router(account_router, prefix="/api/v1")
