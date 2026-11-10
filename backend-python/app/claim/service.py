from uuid import uuid4

from app.claim.store import Claim, InMemoryClaimStore, now_utc
from app.listing import service as listing_service
from app.listing.store import ItemPost
from app.shared.errors import (
    CLAIM_DUPLICATE,
    HANDOVER_CONFLICT,
    INVALID_STATE_TRANSITION,
    NOT_FOUND,
    POST_NOT_CLAIMABLE,
    VALIDATION_FAILED,
    BusinessError,
)

_store = InMemoryClaimStore()

MAX_STATEMENT = 2000
MAX_PAGE_SIZE = 100


def _posts():
    # 与 listing 共享同一内存信息存储（walking skeleton 阶段）。
    return listing_service._store


def submit(applicant_id: str, post_id: str, *, statement: str | None) -> Claim:
    post = _posts().find_by_id(post_id)
    if post is None:
        raise BusinessError(NOT_FOUND)
    if post.type != "FOUND" or post.status not in ("PUBLISHED", "VERIFYING") or post.author_id == applicant_id:
        raise BusinessError(POST_NOT_CLAIMABLE)
    if _store.find_active(applicant_id, post_id) is not None:
        raise BusinessError(CLAIM_DUPLICATE)
    stmt = _require_statement(statement)
    now = now_utc()
    claim = Claim(
        id=str(uuid4()),
        post_id=post_id,
        applicant_id=applicant_id,
        statement=stmt,
        status="SUBMITTED",
        version=1,
        created_at=now,
        updated_at=now,
    )
    _store.save(claim)
    if post.status == "PUBLISHED":
        _posts().save(_with_post_status(post, "VERIFYING", now))
    return claim


def list_by_post(owner_id: str, post_id: str, page: int, page_size: int) -> dict:
    _require_owned_post(owner_id, post_id)
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    claims = _store.list_by_post(post_id)
    total = len(claims)
    start = (page - 1) * page_size
    items = claims[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def request_info(owner_id: str, claim_id: str) -> Claim:
    claim = _require_owned_claim(owner_id, claim_id)
    if claim.status != "SUBMITTED":
        raise BusinessError(INVALID_STATE_TRANSITION)
    return _with_status(claim, "NEEDS_MORE_INFO")


def accept(owner_id: str, claim_id: str) -> Claim:
    claim = _require_owned_claim(owner_id, claim_id)
    if claim.status not in ("SUBMITTED", "NEEDS_MORE_INFO"):
        raise BusinessError(INVALID_STATE_TRANSITION)
    if _store.exists_accepted_on_post(claim.post_id):
        raise BusinessError(HANDOVER_CONFLICT)
    updated = _with_status(claim, "ACCEPTED")
    post = _posts().find_by_id(claim.post_id)
    if post is not None and post.status == "VERIFYING":
        _posts().save(_with_post_status(post, "PENDING_HANDOVER", now_utc()))
    return updated


def reject(owner_id: str, claim_id: str) -> Claim:
    claim = _require_owned_claim(owner_id, claim_id)
    if claim.status not in ("SUBMITTED", "NEEDS_MORE_INFO"):
        raise BusinessError(INVALID_STATE_TRANSITION)
    updated = _with_status(claim, "REJECTED")
    post = _posts().find_by_id(claim.post_id)
    if post is not None and post.status == "VERIFYING" and not _store.has_other_active(claim.post_id, claim_id):
        _posts().save(_with_post_status(post, "PUBLISHED", now_utc()))
    return updated


def _require_owned_claim(owner_id: str, claim_id: str) -> Claim:
    claim = _store.find_by_id(claim_id)
    if claim is None:
        raise BusinessError(NOT_FOUND)
    post = _posts().find_by_id(claim.post_id)
    if post is None or post.author_id != owner_id:
        raise BusinessError(NOT_FOUND)
    return claim


def _require_owned_post(owner_id: str, post_id: str) -> None:
    post = _posts().find_by_id(post_id)
    if post is None or post.author_id != owner_id:
        raise BusinessError(NOT_FOUND)


def _require_statement(statement: str | None) -> str:
    if statement is None or not statement.strip() or len(statement.strip()) > MAX_STATEMENT:
        raise BusinessError(VALIDATION_FAILED)
    return statement.strip()


def _with_status(claim: Claim, status: str) -> Claim:
    updated = Claim(
        id=claim.id,
        post_id=claim.post_id,
        applicant_id=claim.applicant_id,
        statement=claim.statement,
        status=status,
        version=claim.version,
        created_at=claim.created_at,
        updated_at=now_utc(),
    )
    _store.save(updated)
    return updated


def _with_post_status(post: ItemPost, status: str, now: str) -> ItemPost:
    return ItemPost(
        id=post.id,
        author_id=post.author_id,
        type=post.type,
        category=post.category,
        title=post.title,
        public_description=post.public_description,
        school=post.school,
        campus=post.campus,
        area=post.area,
        status=status,
        created_at=post.created_at,
        updated_at=now,
    )
