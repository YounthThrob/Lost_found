from fastapi import Depends

from app.operations import service
from app.security.deps import get_current_user


def require_operator(user_id: str = Depends(get_current_user)) -> str:
    """运营鉴权依赖：非 OPERATOR 抛 OPERATOR_REQUIRED。"""
    service.require_operator(user_id)
    return user_id
