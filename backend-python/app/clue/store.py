from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class Clue:
    id: str
    post_id: str
    provider_id: str
    content: str
    status: str
    created_at: str


class InMemoryClueStore:
    """内存线索存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, Clue] = {}

    def save(self, clue: Clue) -> None:
        self._by_id[clue.id] = clue

    def find_by_id(self, clue_id: str) -> Clue | None:
        return self._by_id.get(clue_id)

    def list_by_post(self, post_id: str) -> list[Clue]:
        clues = [c for c in self._by_id.values() if c.post_id == post_id]
        clues.sort(key=lambda c: (c.created_at, c.id), reverse=True)
        return clues

    def clear(self) -> None:
        self._by_id.clear()
