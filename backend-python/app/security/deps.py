import jwt as pyjwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.account import service as account_service
from app.security.jwt import parse_subject
from app.shared.errors import SESSION_EXPIRED, BusinessError

_bearer = HTTPBearer(auto_error=False)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> str:
    if credentials is None:
        raise BusinessError(SESSION_EXPIRED)
    try:
        user_id = parse_subject(credentials.credentials)
    except pyjwt.PyJWTError:
        raise BusinessError(SESSION_EXPIRED)
    # 会话有效性：JWT 签名与有效期之外，还需确认用户仍存在（内存库重建/用户注销后应判定为会话失效）。
    if account_service.current_user(user_id) is None:
        raise BusinessError(SESSION_EXPIRED)
    return user_id
