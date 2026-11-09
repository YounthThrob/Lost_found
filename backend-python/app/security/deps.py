import jwt as pyjwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.security.jwt import parse_subject
from app.shared.errors import SESSION_EXPIRED, BusinessError

_bearer = HTTPBearer(auto_error=False)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> str:
    if credentials is None:
        raise BusinessError(SESSION_EXPIRED)
    try:
        return parse_subject(credentials.credentials)
    except pyjwt.PyJWTError:
        raise BusinessError(SESSION_EXPIRED)
