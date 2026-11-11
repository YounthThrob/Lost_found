from uuid import uuid4

from app.abuse.rate_limiter import check as rate_limit_check
from app.clue.store import Clue, InMemoryClueStore, now_utc
from app.listing import service as listing_service
from app.moderation.service import is_rejected
from app.shared.errors import (
    CLUE_NOT_ALLOWED,
    INVALID_STATE_TRANSITION,
    NOT_FOUND,
    VALIDATION_FAILED,
    BusinessError,
)

_store = InMemoryClueStore()

MAX_CONTENT = 2000
MAX_PAGE_SIZE = 100


def _posts():
    # 与 listing 共享同一内存信息存储（walking skeleton 阶段）。
    return listing_service._store


def submit(provider_id: str, post_id: str, *, content: str | None) -> Clue:
    rate_limit_check(provider_id, "clue")
    post = _posts().find_by_id(post_id)
    if post is None or post.status not in ("PUBLISHED", "VERIFYING"):
        raise BusinessError(NOT_FOUND)
    if post.type != "LOST":
        raise BusinessError(CLUE_NOT_ALLOWED)
    text = _require_content(content)
    status = "REJECTED" if is_rejected(text, None) else "SUBMITTED"
    clue = Clue(
        id=str(uuid4()),
        post_id=post_id,
        provider_id=provider_id,
        content=text,
        status=status,
        created_at=now_utc(),
    )
    _store.save(clue)
    return clue


def list_pending_by_post(owner_id: str, post_id: str, page: int, page_size: int) -> dict:
    _require_owned_post(owner_id, post_id)
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    clues = [c for c in _store.list_by_post(post_id) if c.status == "SUBMITTED"]
    total = len(clues)
    start = (page - 1) * page_size
    items = clues[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def decide(owner_id: str, clue_id: str, *, decision: str | None) -> Clue:
    clue = _require_owned_clue(owner_id, clue_id)
    if clue.status != "SUBMITTED":
        raise BusinessError(INVALID_STATE_TRANSITION)
    d = (decision or "").strip()
    if d not in ("VALID", "INVALID"):
        raise BusinessError(VALIDATION_FAILED)
    return _with_status(clue, "ACCEPTED" if d == "VALID" else "CLOSED")


def _require_owned_clue(owner_id: str, clue_id: str) -> Clue:
    clue = _store.find_by_id(clue_id)
    if clue is None:
        raise BusinessError(NOT_FOUND)
    post = _posts().find_by_id(clue.post_id)
    if post is None or post.author_id != owner_id:
        raise BusinessError(NOT_FOUND)
    return clue


def _require_owned_post(owner_id: str, post_id: str) -> None:
    post = _posts().find_by_id(post_id)
    if post is None or post.author_id != owner_id:
        raise BusinessError(NOT_FOUND)


def _require_content(content: str | None) -> str:
    if content is None or not content.strip() or len(content.strip()) > MAX_CONTENT:
        raise BusinessError(VALIDATION_FAILED)
    return content.strip()


def _with_status(clue: Clue, status: str) -> Clue:
    updated = Clue(
        id=clue.id,
        post_id=clue.post_id,
        provider_id=clue.provider_id,
        content=clue.content,
        status=status,
        created_at=clue.created_at,
    )
    _store.save(updated)
    return updated
