from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class Bookmark:
    user_id: str
    post_id: str
    created_at: str


class InMemoryBookmarkStore:
    """内存收藏存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_key: dict[str, Bookmark] = {}

    @staticmethod
    def _key(user_id: str, post_id: str) -> str:
        return f"{user_id}:{post_id}"

    def find(self, user_id: str, post_id: str) -> Bookmark | None:
        return self._by_key.get(self._key(user_id, post_id))

    def save(self, bookmark: Bookmark) -> None:
        self._by_key[self._key(bookmark.user_id, bookmark.post_id)] = bookmark

    def remove(self, user_id: str, post_id: str) -> None:
        self._by_key.pop(self._key(user_id, post_id), None)

    def list_by_user(self, user_id: str) -> list[Bookmark]:
        bookmarks = [b for b in self._by_key.values() if b.user_id == user_id]
        bookmarks.sort(key=lambda b: (b.created_at, b.post_id), reverse=True)
        return bookmarks

    def clear(self) -> None:
        self._by_key.clear()
