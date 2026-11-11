from uuid import uuid4

from app.account import service as account_service
from app.account.store import UserAccount
from app.listing import service as listing_service
from app.listing.store import ItemPost
from app.moderation import service as moderation_service
from app.moderation.store import Report
from app.operations.store import AuditEvent, InMemoryAuditStore, now_utc
from app.shared.errors import (
    NOT_FOUND,
    OPERATOR_REQUIRED,
    VALIDATION_FAILED,
    BusinessError,
)

_store = InMemoryAuditStore()

MAX_REASON = 200
MAX_PAGE_SIZE = 100


def require_operator(user_id: str) -> UserAccount:
    """运营鉴权守卫：非 OPERATOR 抛 OPERATOR_REQUIRED。"""
    user = account_service.current_user(user_id)
    if user is None or not user.is_operator:
        raise BusinessError(OPERATOR_REQUIRED)
    return user


def remove_post(operator_id: str, post_id: str, reason: str) -> ItemPost:
    _require_reason(reason)
    post = listing_service.remove(post_id)
    _audit(operator_id, "POST_REMOVED", "POST", post_id, reason)
    return post


def restrict_user(operator_id: str, user_id: str, reason: str) -> UserAccount:
    _require_reason(reason)
    user = account_service.restrict_user(user_id)
    if user is None:
        raise BusinessError(NOT_FOUND)
    _audit(operator_id, "USER_RESTRICTED", "USER", user_id, reason)
    return user


def unrestrict_user(operator_id: str, user_id: str, reason: str) -> UserAccount:
    _require_reason(reason)
    user = account_service.unrestrict_user(user_id)
    if user is None:
        raise BusinessError(NOT_FOUND)
    _audit(operator_id, "USER_UNRESTRICTED", "USER", user_id, reason)
    return user


def list_reports(page: int, page_size: int) -> dict:
    _validate_page(page, page_size)
    reports: list[Report] = moderation_service.list_all_reports()
    total = len(reports)
    start = (page - 1) * page_size
    items = reports[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def list_audit_events(page: int, page_size: int) -> dict:
    _validate_page(page, page_size)
    events = _store.list_all()
    total = len(events)
    start = (page - 1) * page_size
    items = events[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def _audit(operator_id: str, event_type: str, target_type: str, target_id: str, reason: str) -> None:
    _store.save(AuditEvent(
        id=str(uuid4()),
        actor_id=operator_id,
        event_type=event_type,
        target_type=target_type,
        target_id=target_id,
        reason=reason.strip(),
        created_at=now_utc(),
    ))


def _require_reason(reason: str | None) -> None:
    if reason is None or not reason.strip() or len(reason.strip()) > MAX_REASON:
        raise BusinessError(VALIDATION_FAILED)


def _validate_page(page: int, page_size: int) -> None:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
