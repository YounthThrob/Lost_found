from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class BlockRelation:
    blocker_id: str
    blocked_id: str
    created_at: str


@dataclass
class Message:
    id: str
    conversation_id: str
    sender_id: str
    type: str
    content: str
    created_at: str


class InMemoryBlockStore:
    """内存拉黑关系存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_blocker: dict[str, dict[str, BlockRelation]] = {}

    def find(self, blocker_id: str, blocked_id: str) -> BlockRelation | None:
        inner = self._by_blocker.get(blocker_id)
        return inner.get(blocked_id) if inner else None

    def put(self, relation: BlockRelation) -> BlockRelation:
        self._by_blocker.setdefault(relation.blocker_id, {})[relation.blocked_id] = relation
        return relation

    def remove(self, blocker_id: str, blocked_id: str) -> None:
        inner = self._by_blocker.get(blocker_id)
        if inner:
            inner.pop(blocked_id, None)

    def list_by_blocker(self, blocker_id: str) -> list[BlockRelation]:
        inner = self._by_blocker.get(blocker_id)
        if not inner:
            return []
        items = list(inner.values())
        items.sort(key=lambda r: r.created_at, reverse=True)
        return items

    def clear(self) -> None:
        self._by_blocker.clear()


class InMemoryMessageStore:
    """内存消息存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, Message] = {}

    def save(self, message: Message) -> None:
        self._by_id[message.id] = message

    def list_by_conversation(self, conversation_id: str) -> list[Message]:
        items = [m for m in self._by_id.values() if m.conversation_id == conversation_id]
        items.sort(key=lambda m: m.created_at, reverse=True)
        return items

    def clear(self) -> None:
        self._by_id.clear()
