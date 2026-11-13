from uuid import uuid4

from app.claim import service as claim_service
from app.claim.store import Claim
from app.handover.store import Handover, InMemoryHandoverStore, now_utc
from app.listing import service as listing_service
from app.listing.store import ItemPost
from app.shared.errors import (
    HANDOVER_ALREADY_EXISTS,
    INVALID_STATE_TRANSITION,
    NOT_FOUND,
    VALIDATION_FAILED,
    BusinessError,
)

_store = InMemoryHandoverStore()

MAX_FIELD = 200


def _claims():
    # 与 claim 共享同一内存申请存储。
    return claim_service._store


def _posts():
    # 与 listing 共享同一内存信息存储。
    return listing_service._store


def create(user_id: str, *, claim_id: str | None, time_window: str | None, public_place: str | None) -> Handover:
    claim = _require_claim(claim_id)
    post = _require_post(claim.post_id)
    _require_participant(user_id, claim, post)
    if claim.status != "ACCEPTED":
        raise BusinessError(INVALID_STATE_TRANSITION)
    if _store.find_by_claim_id(claim.id) is not None:
        raise BusinessError(HANDOVER_ALREADY_EXISTS)
    tw = _require_field(time_window)
    pp = _require_field(public_place)
    handover = Handover(
        id=str(uuid4()),
        claim_id=claim.id,
        proposer_id=user_id,
        time_window=tw,
        public_place=pp,
        owner_confirmed_at=None,
        claimant_confirmed_at=None,
        status="PENDING",
        created_at=now_utc(),
    )
    _store.save(handover)
    return handover


def confirm(user_id: str, handover_id: str) -> Handover:
    handover = _require_handover(handover_id)
    claim = _require_claim(handover.claim_id)
    post = _require_post(claim.post_id)
    _require_participant(user_id, claim, post)
    if handover.status != "PENDING":
        raise BusinessError(INVALID_STATE_TRANSITION)
    now = now_utc()
    owner_confirmed_at = handover.owner_confirmed_at
    claimant_confirmed_at = handover.claimant_confirmed_at
    if user_id == post.author_id and owner_confirmed_at is None:
        owner_confirmed_at = now
    if user_id == claim.applicant_id and claimant_confirmed_at is None:
        claimant_confirmed_at = now
    completed = owner_confirmed_at is not None and claimant_confirmed_at is not None
    updated = Handover(
        id=handover.id,
        claim_id=handover.claim_id,
        proposer_id=handover.proposer_id,
        time_window=handover.time_window,
        public_place=handover.public_place,
        owner_confirmed_at=owner_confirmed_at,
        claimant_confirmed_at=claimant_confirmed_at,
        status="COMPLETED" if completed else "PENDING",
        created_at=handover.created_at,
    )
    _store.save(updated)
    if completed:
        _claims().save(_with_claim_status(claim, "COMPLETED"))
        if post.status == "PENDING_HANDOVER":
            _posts().save(_with_post_status(post, "COMPLETED"))
    return updated


def cancel(user_id: str, handover_id: str) -> Handover:
    handover = _require_handover(handover_id)
    claim = _require_claim(handover.claim_id)
    post = _require_post(claim.post_id)
    _require_participant(user_id, claim, post)
    if handover.status != "PENDING":
        raise BusinessError(INVALID_STATE_TRANSITION)
    updated = Handover(
        id=handover.id,
        claim_id=handover.claim_id,
        proposer_id=handover.proposer_id,
        time_window=handover.time_window,
        public_place=handover.public_place,
        owner_confirmed_at=handover.owner_confirmed_at,
        claimant_confirmed_at=handover.claimant_confirmed_at,
        status="CANCELLED",
        created_at=handover.created_at,
    )
    _store.save(updated)
    _claims().save(_with_claim_status(claim, "CANCELLED"))
    if post.status == "PENDING_HANDOVER":
        _posts().save(_with_post_status(post, "PUBLISHED"))
    return updated


def _require_handover(handover_id: str) -> Handover:
    handover = _store.find_by_id(handover_id)
    if handover is None:
        raise BusinessError(NOT_FOUND)
    return handover


def _require_claim(claim_id: str | None):
    if claim_id is None:
        raise BusinessError(NOT_FOUND)
    claim = _claims().find_by_id(claim_id)
    if claim is None:
        raise BusinessError(NOT_FOUND)
    return claim


def _require_post(post_id: str):
    post = _posts().find_by_id(post_id)
    if post is None:
        raise BusinessError(NOT_FOUND)
    return post


def _require_participant(user_id: str, claim, post) -> None:
    if user_id != post.author_id and user_id != claim.applicant_id:
        raise BusinessError(NOT_FOUND)


def _require_field(value: str | None) -> str:
    if value is None or not value.strip() or len(value.strip()) > MAX_FIELD:
        raise BusinessError(VALIDATION_FAILED)
    return value.strip()


def _with_claim_status(claim: Claim, status: str) -> Claim:
    return Claim(
        id=claim.id,
        post_id=claim.post_id,
        applicant_id=claim.applicant_id,
        statement=claim.statement,
        status=status,
        version=claim.version,
        created_at=claim.created_at,
        updated_at=now_utc(),
    )


def _with_post_status(post: ItemPost, status: str) -> ItemPost:
    return ItemPost(
        id=post.id,
        author_id=post.author_id,
        type=post.type,
        category=post.category,
        title=post.title,
        public_description=post.public_description,
        school_id=post.school_id,
        school_name=post.school_name,
        campus_id=post.campus_id,
        campus_name=post.campus_name,
        area=post.area,
        status=status,
        created_at=post.created_at,
        updated_at=now_utc(),
    )
