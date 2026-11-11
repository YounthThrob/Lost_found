from app.account.store import InMemoryUserStore, UserAccount
from app.account.wechat_client import code2session_openid
from app.security.jwt import issue_token
from app.shared.errors import ACCOUNT_RESTRICTED, AGREEMENT_NOT_ACCEPTED, VALIDATION_FAILED, BusinessError

_store = InMemoryUserStore()

MAX_FIELD_LENGTH = 100


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
    """受限操作守卫：未确认协议抛 AGREEMENT_NOT_ACCEPTED，处于限制状态抛 ACCOUNT_RESTRICTED。"""
    user = _store.find_by_id(user_id)
    if user is None or not user.agreement_accepted:
        raise BusinessError(AGREEMENT_NOT_ACCEPTED)
    if user.status == "RESTRICTED":
        raise BusinessError(ACCOUNT_RESTRICTED)
    return user


def update_campus_profile(user_id: str, school: str | None, campus: str | None) -> UserAccount:
    normalized_school = (school or "").strip()
    normalized_campus = campus.strip() if campus and campus.strip() else None
    if not normalized_school:
        raise BusinessError(VALIDATION_FAILED)
    if len(normalized_school) > MAX_FIELD_LENGTH or (
        normalized_campus and len(normalized_campus) > MAX_FIELD_LENGTH
    ):
        raise BusinessError(VALIDATION_FAILED)
    return _store.update_campus(user_id, normalized_school, normalized_campus)
