from app.bookmark.store import Bookmark, InMemoryBookmarkStore, now_utc
from app.listing import service as listing_service
from app.shared.errors import NOT_FOUND, VALIDATION_FAILED, BusinessError

_store = InMemoryBookmarkStore()

MAX_PAGE_SIZE = 100


def _posts():
    # 与 listing 共享同一内存信息存储。
    return listing_service._store


def bookmark(user_id: str, post_id: str) -> dict:
    post = _posts().find_by_id(post_id)
    if post is None or post.status not in ("PUBLISHED", "VERIFYING"):
        raise BusinessError(NOT_FOUND)
    existing = _store.find(user_id, post_id)
    if existing is None:
        existing = Bookmark(user_id=user_id, post_id=post_id, created_at=now_utc())
        _store.save(existing)
    return {"postId": existing.post_id, "createdAt": existing.created_at, "post": post}


def unbookmark(user_id: str, post_id: str) -> None:
    _store.remove(user_id, post_id)


def list_bookmarks(user_id: str, page: int, page_size: int) -> dict:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    entries = []
    for b in _store.list_by_user(user_id):
        post = _posts().find_by_id(b.post_id)
        if post is not None:
            entries.append({"postId": b.post_id, "createdAt": b.created_at, "post": post})
    total = len(entries)
    start = (page - 1) * page_size
    items = entries[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }
