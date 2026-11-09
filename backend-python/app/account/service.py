from app.account.store import InMemoryUserStore, UserAccount
from app.account.wechat_client import code2session_openid
from app.security.jwt import issue_token

_store = InMemoryUserStore()


def login(code: str) -> str:
    openid = code2session_openid(code)
    user = _store.find_by_openid(openid)
    return issue_token(user.id)


def current_user(user_id: str) -> UserAccount | None:
    return _store.find_by_id(user_id)
