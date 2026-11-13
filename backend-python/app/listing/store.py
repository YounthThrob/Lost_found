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


@dataclass
class ItemImage:
    id: str
    post_id: str
    storage_key: str
    sort_order: int
    moderation_status: str


class InMemoryItemImageStore:
    """内存图片元数据存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, ItemImage] = {}

    def find_by_id(self, image_id: str) -> ItemImage | None:
        return self._by_id.get(image_id)

    def save(self, image: ItemImage) -> None:
        self._by_id[image.id] = image

    def list_by_post(self, post_id: str) -> list[ItemImage]:
        images = [i for i in self._by_id.values() if i.post_id == post_id]
        images.sort(key=lambda i: (i.sort_order, i.id))
        return images

    def delete(self, image_id: str) -> None:
        self._by_id.pop(image_id, None)

    def clear(self) -> None:
        self._by_id.clear()


class InMemoryItemPostStore:
    """内存信息存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, ItemPost] = {}

    def find_by_id(self, post_id: str) -> ItemPost | None:
        return self._by_id.get(post_id)

    def save(self, post: ItemPost) -> None:
        self._by_id[post.id] = post

    def list_visible(self) -> list[ItemPost]:
        posts = [p for p in self._by_id.values() if p.status in ("PUBLISHED", "VERIFYING")]
        posts.sort(key=lambda p: (p.created_at, p.id), reverse=True)
        return posts

    def list_by_author(self, author_id: str) -> list[ItemPost]:
        posts = [p for p in self._by_id.values() if p.author_id == author_id]
        posts.sort(key=lambda p: p.id)
        posts.sort(key=lambda p: p.created_at, reverse=True)
        return posts

    def clear(self) -> None:
        self._by_id.clear()
