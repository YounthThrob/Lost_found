from fastapi import Depends

from app.account import service
from app.security.deps import get_current_user


def require_agreement(user_id: str = Depends(get_current_user)) -> str:
    """受限操作守卫：未确认协议时抛 AGREEMENT_NOT_ACCEPTED。"""
    service.require_agreement(user_id)
    return user_id
