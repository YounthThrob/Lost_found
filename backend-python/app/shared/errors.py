from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


@dataclass(frozen=True)
class ErrorCode:
    code: str
    http_status: int
    message: str


# 必须与 contracts/error-codes.yaml 保持一致。
SESSION_EXPIRED = ErrorCode("SESSION_EXPIRED", 401, "会话已过期，请重新登录")
WECHAT_CODE_INVALID = ErrorCode("WECHAT_CODE_INVALID", 401, "微信登录凭证无效或已过期")
AGREEMENT_NOT_ACCEPTED = ErrorCode("AGREEMENT_NOT_ACCEPTED", 403, "未确认用户协议，禁止发布/留言/认领")
VALIDATION_FAILED = ErrorCode("VALIDATION_FAILED", 422, "参数校验失败")
NOT_FOUND = ErrorCode("NOT_FOUND", 404, "资源不存在或无权访问")
INVALID_STATE_TRANSITION = ErrorCode("INVALID_STATE_TRANSITION", 409, "非法状态迁移")
CLAIM_DUPLICATE = ErrorCode("CLAIM_DUPLICATE", 409, "同一物品已存在进行中的申请")
POST_NOT_CLAIMABLE = ErrorCode("POST_NOT_CLAIMABLE", 409, "信息当前不可申请认领")
HANDOVER_CONFLICT = ErrorCode("HANDOVER_CONFLICT", 409, "已有申请进入待交接")
BLOCKED = ErrorCode("BLOCKED", 403, "已被对方拉黑，无法继续发送消息或申请")
INTERNAL_ERROR = ErrorCode("INTERNAL_ERROR", 500, "服务内部错误")


class BusinessError(Exception):
    def __init__(self, error_code: ErrorCode):
        self.error_code = error_code
        super().__init__(error_code.message)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def envelope(data) -> dict:
    return {"data": data, "requestId": str(uuid4()), "timestamp": _now()}


def error_response(error_code: ErrorCode) -> JSONResponse:
    body = {
        "error": {"code": error_code.code, "message": error_code.message, "details": {}},
        "requestId": str(uuid4()),
        "timestamp": _now(),
    }
    return JSONResponse(status_code=error_code.http_status, content=body)


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(BusinessError)
    async def _business(request: Request, exc: BusinessError):
        return error_response(exc.error_code)

    @app.exception_handler(StarletteHTTPException)
    async def _http(request: Request, exc: StarletteHTTPException):
        return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})

    @app.exception_handler(RequestValidationError)
    async def _validation(request: Request, exc: RequestValidationError):
        return error_response(VALIDATION_FAILED)

    @app.exception_handler(Exception)
    async def _unexpected(request: Request, exc: Exception):
        return error_response(INTERNAL_ERROR)
