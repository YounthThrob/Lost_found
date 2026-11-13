import re
from uuid import uuid4

from app.account.store import InMemoryUserStore, UserAccount
from app.account.verification import EmailProvider
from app.account.wechat_client import code2session_openid
from app.config import CAMPUS_EMAIL_DOMAINS
from app.security.jwt import issue_token
from app.storage import LocalFileStorage
from app.shared.errors import (
    ACCOUNT_RESTRICTED,
    AGREEMENT_NOT_ACCEPTED,
    VALIDATION_FAILED,
    VERIFICATION_CODE_INVALID,
    BusinessError,
)

_store = InMemoryUserStore()
_email_provider = EmailProvider(CAMPUS_EMAIL_DOMAINS)
_storage = LocalFileStorage()

MAX_FIELD_LENGTH = 100
MAX_NICKNAME_LENGTH = 30
MAX_AVATAR_BYTES = 10 * 1024 * 1024
ALLOWED_AVATAR_TYPES = {"image/jpeg", "image/png", "image/webp"}

OPERATOR_OPENID = "openid_operator_001"

_EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def login(code: str) -> tuple[str, bool]:
    """登录，返回 (token, agreement_accepted)。"""
    openid = code2session_openid(code)
    role = "OPERATOR" if openid == OPERATOR_OPENID else "USER"
    user = _store.find_by_openid(openid, role)
    return issue_token(user.id), user.agreement_accepted


def current_user(user_id: str) -> UserAccount | None:
    return _store.find_by_id(user_id)


def restrict_user(user_id: str) -> UserAccount | None:
    return _store.restrict(user_id)


def unrestrict_user(user_id: str) -> UserAccount | None:
    return _store.unrestrict(user_id)


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


def send_campus_email_verification(user_id: str, email: str | None) -> int:
    return _email_provider.send(_normalize_email(email))


def confirm_campus_email_verification(user_id: str, email: str | None, code: str | None) -> UserAccount:
    normalized = _normalize_email(email)
    if not code or not code.strip():
        raise BusinessError(VALIDATION_FAILED)
    if not _email_provider.confirm(normalized, code):
        raise BusinessError(VERIFICATION_CODE_INVALID)
    user = _store.verify_email(user_id, normalized)
    if user is None:
        raise BusinessError(VALIDATION_FAILED)
    return user


def _normalize_email(email: str | None) -> str:
    if email is None:
        raise BusinessError(VALIDATION_FAILED)
    normalized = email.strip()
    if len(normalized) > MAX_FIELD_LENGTH or not _EMAIL_RE.match(normalized):
        raise BusinessError(VALIDATION_FAILED)
    return normalized


def avatar_url(avatar_key: str | None) -> str | None:
    return "/media/" + avatar_key if avatar_key else None


def profile_view(user: UserAccount) -> dict:
    return {"nickname": user.nickname, "avatarUrl": avatar_url(user.avatar_key)}


def update_profile(user_id: str, nickname: str | None) -> UserAccount:
    normalized = (nickname or "").strip()
    if not normalized:
        raise BusinessError(VALIDATION_FAILED)
    if len(normalized) > MAX_NICKNAME_LENGTH:
        raise BusinessError(VALIDATION_FAILED)
    user = _store.find_by_id(user_id)
    if user is None:
        raise BusinessError(VALIDATION_FAILED)
    return _store.update_profile(user_id, normalized, user.avatar_key)


def set_avatar(user_id: str, content: bytes | None, content_type: str | None) -> str:
    normalized_type = (content_type or "").strip()
    if normalized_type not in ALLOWED_AVATAR_TYPES:
        raise BusinessError(VALIDATION_FAILED)
    if content is None or len(content) > MAX_AVATAR_BYTES:
        raise BusinessError(VALIDATION_FAILED)
    user = _store.find_by_id(user_id)
    if user is None:
        raise BusinessError(VALIDATION_FAILED)
    key = str(uuid4())
    _storage.put(key, content, normalized_type)
    _store.update_profile(user_id, user.nickname, key)
    if user.avatar_key and user.avatar_key != key:
        _storage.delete(user.avatar_key)
    return "/media/" + key
