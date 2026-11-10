from app.account.store import InMemoryUserStore, UserAccount
from app.account.wechat_client import code2session_openid
from app.security.jwt import issue_token
from app.shared.errors import AGREEMENT_NOT_ACCEPTED, BusinessError

_store = InMemoryUserStore()


def login(code: str) -> tuple[str, bool]:
    """登录，返回 (token, agreement_accepted)。"""
    openid = code2session_openid(code)
    user = _store.find_by_openid(openid)
    return issue_token(user.id), user.agreement_accepted


def current_user(user_id: str) -> UserAccount | None:
    return _store.find_by_id(user_id)


def confirm_agreement(user_id: str) -> UserAccount | None:
    return _store.confirm_agreement(user_id)


def require_agreement(user_id: str) -> UserAccount:
    """受限操作守卫：未确认协议时抛 AGREEMENT_NOT_ACCEPTED。"""
    user = _store.find_by_id(user_id)
    if user is None or not user.agreement_accepted:
        raise BusinessError(AGREEMENT_NOT_ACCEPTED)
    return user
