from uuid import uuid4

from app.abuse.rate_limiter import check as rate_limit_check
from app.account import service as account_service
from app.claim import service as claim_service
from app.communication.store import (
    BlockRelation,
    InMemoryBlockStore,
    InMemoryMessageStore,
    Message,
    now_utc,
)
from app.listing import service as listing_service
from app.shared.errors import (
    BLOCKED,
    INVALID_STATE_TRANSITION,
    NOT_FOUND,
    VALIDATION_FAILED,
    BusinessError,
)

_block_store = InMemoryBlockStore()
_message_store = InMemoryMessageStore()

MAX_CONTENT = 2000
MAX_PAGE_SIZE = 100
MESSAGE_TYPES = ("TEXT", "SUGGEST_TIME", "SUGGEST_PLACE", "LATE_REMINDER")


def _users():
    # 与 account 共享同一内存用户存储。
    return account_service._store


def _claims():
    # 与 claim 共享同一内存申请存储。
    return claim_service._store


def _posts():
    # 与 listing 共享同一内存信息存储。
    return listing_service._store


def block(blocker_id: str, blocked_id: str | None) -> BlockRelation:
    if not blocked_id or not blocked_id.strip():
        raise BusinessError(VALIDATION_FAILED)
    if blocker_id == blocked_id:
        raise BusinessError(VALIDATION_FAILED)
    if _users().find_by_id(blocked_id) is None:
        raise BusinessError(NOT_FOUND)
    existing = _block_store.find(blocker_id, blocked_id)
    if existing is not None:
        return existing
    return _block_store.put(BlockRelation(blocker_id, blocked_id, now_utc()))


def unblock(blocker_id: str, blocked_id: str) -> None:
    _block_store.remove(blocker_id, blocked_id)


def list_blocks(blocker_id: str, page: int, page_size: int) -> dict:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    all_blocks = _block_store.list_by_blocker(blocker_id)
    total = len(all_blocks)
    start = (page - 1) * page_size
    items = all_blocks[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def send(sender_id: str, conversation_id: str, *, type_: str | None, content: str | None) -> Message:
    rate_limit_check(sender_id, "message")
    claim = _claims().find_by_id(conversation_id)
    if claim is None:
        raise BusinessError(NOT_FOUND)
    post = _posts().find_by_id(claim.post_id)
    if post is None:
        raise BusinessError(NOT_FOUND)
    owner_id = post.author_id
    applicant_id = claim.applicant_id
    if sender_id not in (owner_id, applicant_id):
        raise BusinessError(NOT_FOUND)
    if claim.status != "ACCEPTED":
        raise BusinessError(INVALID_STATE_TRANSITION)
    recipient_id = applicant_id if sender_id == owner_id else owner_id
    if _block_store.find(recipient_id, sender_id) is not None:
        raise BusinessError(BLOCKED)
    type_val = _require_type(type_)
    content_val = _require_content(content)
    message = Message(
        id=str(uuid4()),
        conversation_id=conversation_id,
        sender_id=sender_id,
        type=type_val,
        content=content_val,
        created_at=now_utc(),
    )
    _message_store.save(message)
    return message


def list_messages(viewer_id: str, conversation_id: str, page: int, page_size: int) -> dict:
    claim = _claims().find_by_id(conversation_id)
    if claim is None:
        raise BusinessError(NOT_FOUND)
    post = _posts().find_by_id(claim.post_id)
    if post is None:
        raise BusinessError(NOT_FOUND)
    owner_id = post.author_id
    applicant_id = claim.applicant_id
    if viewer_id not in (owner_id, applicant_id):
        raise BusinessError(NOT_FOUND)
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    all_messages = _message_store.list_by_conversation(conversation_id)
    total = len(all_messages)
    start = (page - 1) * page_size
    items = all_messages[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def _require_type(type_: str | None) -> str:
    if type_ is None or type_ not in MESSAGE_TYPES:
        raise BusinessError(VALIDATION_FAILED)
    return type_


def _require_content(content: str | None) -> str:
    if content is None or not content.strip() or len(content.strip()) > MAX_CONTENT:
        raise BusinessError(VALIDATION_FAILED)
    return content.strip()
