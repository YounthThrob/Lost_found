from datetime import datetime, timedelta, timezone
from uuid import uuid4

from app.abuse.rate_limiter import check as rate_limit_check
from app.listing.store import InMemoryItemImageStore, InMemoryItemPostStore, ItemImage, ItemPost, now_utc
from app.moderation.service import is_rejected
from app.shared.errors import (
    INVALID_STATE_TRANSITION,
    NOT_FOUND,
    VALIDATION_FAILED,
    BusinessError,
)
from app.storage import LocalFileStorage

_store = InMemoryItemPostStore()
_image_store = InMemoryItemImageStore()
_storage = LocalFileStorage()

MAX_TITLE = 100
MAX_CATEGORY = 50
MAX_DESCRIPTION = 2000
MAX_FIELD = 100
MAX_PAGE_SIZE = 100
MAX_IMAGE_BYTES = 10 * 1024 * 1024
MAX_IMAGES_PER_POST = 9
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}


def create_draft(author_id: str, *, post_type: str, category: str, title: str,
                 public_description: str, school: str, campus: str | None,
                 area: str | None) -> ItemPost:
    rate_limit_check(author_id, "post")
    normalized_type = _normalize_type(post_type)
    _validate(normalized_type, category, title, public_description, school, campus, area)
    now = now_utc()
    post = ItemPost(
        id=str(uuid4()),
        author_id=author_id,
        type=normalized_type,
        category=category.strip(),
        title=title.strip(),
        public_description=public_description.strip(),
        school=school.strip(),
        campus=_normalize(campus),
        area=_normalize(area),
        status="DRAFT",
        created_at=now,
        updated_at=now,
    )
    _store.save(post)
    return post


def update_draft(author_id: str, post_id: str, *, post_type: str, category: str, title: str,
                 public_description: str, school: str, campus: str | None,
                 area: str | None) -> ItemPost:
    post = _require_owned(author_id, post_id)
    if post.status != "DRAFT":
        raise BusinessError(INVALID_STATE_TRANSITION)
    normalized_type = _normalize_type(post_type)
    _validate(normalized_type, category, title, public_description, school, campus, area)
    updated = ItemPost(
        id=post.id,
        author_id=post.author_id,
        type=normalized_type,
        category=category.strip(),
        title=title.strip(),
        public_description=public_description.strip(),
        school=school.strip(),
        campus=_normalize(campus),
        area=_normalize(area),
        status=post.status,
        created_at=post.created_at,
        updated_at=now_utc(),
    )
    _store.save(updated)
    return updated


def publish(author_id: str, post_id: str) -> ItemPost:
    post = _require_owned(author_id, post_id)
    if post.status != "DRAFT":
        raise BusinessError(INVALID_STATE_TRANSITION)
    status = "REJECTED" if is_rejected(post.title, post.public_description) else "PUBLISHED"
    return _with_status(post, status)


def close(author_id: str, post_id: str) -> ItemPost:
    post = _require_owned(author_id, post_id)
    if post.status != "PUBLISHED":
        raise BusinessError(INVALID_STATE_TRANSITION)
    return _with_status(post, "CLOSED")


def remove(post_id: str) -> ItemPost:
    """运营下架：仅公开状态（PUBLISHED/VERIFYING/PENDING_HANDOVER）可迁移为 REMOVED。"""
    post = _store.find_by_id(post_id)
    if post is None:
        raise BusinessError(NOT_FOUND)
    if post.status not in ("PUBLISHED", "VERIFYING", "PENDING_HANDOVER"):
        raise BusinessError(INVALID_STATE_TRANSITION)
    return _with_status(post, "REMOVED")


def list_my_posts(author_id: str, page: int, page_size: int) -> dict:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    posts = _store.list_by_author(author_id)
    total = len(posts)
    start = (page - 1) * page_size
    items = posts[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def get_detail(viewer_id: str | None, post_id: str) -> ItemPost:
    post = _store.find_by_id(post_id)
    if post is None:
        raise BusinessError(NOT_FOUND)
    if post.status not in ("PUBLISHED", "VERIFYING") and post.author_id != viewer_id:
        raise BusinessError(NOT_FOUND)
    return post


def search(keyword: str | None, post_type: str | None, category: str | None,
           school: str | None, campus: str | None, page: int, page_size: int) -> dict:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    kw = keyword.strip().lower() if keyword else None
    posts = [
        p for p in _store.list_visible()
        if _matches(post_type, p.type)
        and _matches(category, p.category)
        and _matches(school, p.school)
        and _matches(campus, p.campus)
        and (kw is None or kw in p.title.lower() or kw in p.public_description.lower())
    ]
    total = len(posts)
    start = (page - 1) * page_size
    items = posts[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


_SHANGHAI = timezone(timedelta(hours=8))


def _is_today_beijing(iso_utc: str | None) -> bool:
    if not iso_utc:
        return False
    try:
        dt = datetime.fromisoformat(iso_utc.replace("Z", "+00:00"))
    except ValueError:
        return False
    return dt.astimezone(_SHANGHAI).date() == datetime.now(timezone.utc).astimezone(_SHANGHAI).date()


def stats() -> dict:
    """广场统计：招领/寻物公开数量与今日（北京时间 UTC+8）完成交接数量。"""
    visible = _store.list_visible()
    completed_today = sum(
        1 for p in _store.list_all()
        if p.status == "COMPLETED" and _is_today_beijing(p.updated_at)
    )
    return {
        "foundCount": sum(1 for p in visible if p.type == "FOUND"),
        "lostCount": sum(1 for p in visible if p.type == "LOST"),
        "completedToday": completed_today,
    }


def upload_image(author_id: str, post_id: str, content: bytes | None, content_type: str | None) -> ItemImage:
    post = _require_owned(author_id, post_id)
    if post.status != "DRAFT":
        raise BusinessError(INVALID_STATE_TRANSITION)
    normalized_type = (content_type or "").strip()
    if normalized_type not in ALLOWED_IMAGE_TYPES:
        raise BusinessError(VALIDATION_FAILED)
    if content is None or len(content) > MAX_IMAGE_BYTES:
        raise BusinessError(VALIDATION_FAILED)
    sort_order = len(_image_store.list_by_post(post_id))
    if sort_order >= MAX_IMAGES_PER_POST:
        raise BusinessError(VALIDATION_FAILED)
    image_id = str(uuid4())
    storage_key = str(uuid4())
    _storage.put(storage_key, content, normalized_type)
    image = ItemImage(
        id=image_id,
        post_id=post_id,
        storage_key=storage_key,
        sort_order=sort_order,
        moderation_status="PENDING",
    )
    _image_store.save(image)
    return image


def delete_image(author_id: str, post_id: str, image_id: str) -> None:
    post = _require_owned(author_id, post_id)
    if post.status != "DRAFT":
        raise BusinessError(INVALID_STATE_TRANSITION)
    image = _image_store.find_by_id(image_id)
    if image is None or image.post_id != post_id:
        raise BusinessError(NOT_FOUND)
    _storage.delete(image.storage_key)
    _image_store.delete(image_id)


def list_images(post_id: str) -> list[ItemImage]:
    return _image_store.list_by_post(post_id)


def _require_owned(author_id: str, post_id: str) -> ItemPost:
    post = _store.find_by_id(post_id)
    if post is None or post.author_id != author_id:
        raise BusinessError(NOT_FOUND)
    return post


def _validate(post_type: str | None, category: str, title: str, public_description: str,
              school: str, campus: str | None, area: str | None) -> None:
    if post_type not in ("LOST", "FOUND"):
        raise BusinessError(VALIDATION_FAILED)
    _require_text(title, MAX_TITLE)
    _require_text(category, MAX_CATEGORY)
    _require_text(public_description, MAX_DESCRIPTION)
    _require_text(school, MAX_FIELD)
    if campus is not None and len(campus.strip()) > MAX_FIELD:
        raise BusinessError(VALIDATION_FAILED)
    if area is not None and len(area.strip()) > MAX_FIELD:
        raise BusinessError(VALIDATION_FAILED)


def _normalize_type(post_type: str | None) -> str | None:
    return post_type.strip() if post_type else None


def _require_text(value: str | None, max_len: int) -> None:
    if value is None or not value.strip() or len(value.strip()) > max_len:
        raise BusinessError(VALIDATION_FAILED)


def _normalize(value: str | None) -> str | None:
    if value is None or not value.strip():
        return None
    return value.strip()


def _matches(filt: str | None, value: str) -> bool:
    return filt is None or not filt.strip() or filt == value


def _with_status(post: ItemPost, status: str) -> ItemPost:
    updated = ItemPost(
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
        updated_at=now_utc(),
    )
    _store.save(updated)
    return updated
