from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class ItemPost:
    id: str
    author_id: str
    type: str
    category: str
    title: str
    public_description: str
    school: str
    campus: str | None
    area: str | None
    status: str
    created_at: str
    updated_at: str


class InMemoryItemPostStore:
    """内存信息存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, ItemPost] = {}

    def find_by_id(self, post_id: str) -> ItemPost | None:
        return self._by_id.get(post_id)

    def save(self, post: ItemPost) -> None:
        self._by_id[post.id] = post

    def list_published(self) -> list[ItemPost]:
        posts = [p for p in self._by_id.values() if p.status == "PUBLISHED"]
        posts.sort(key=lambda p: p.created_at, reverse=True)
        return posts

    def clear(self) -> None:
        self._by_id.clear()
