from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class AuditEvent:
    id: str
    actor_id: str
    event_type: str
    target_type: str
    target_id: str
    reason: str
    created_at: str


class InMemoryAuditStore:
    """内存审计事件存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, AuditEvent] = {}

    def save(self, event: AuditEvent) -> None:
        self._by_id[event.id] = event

    def list_all(self) -> list[AuditEvent]:
        events = list(self._by_id.values())
        events.sort(key=lambda e: e.id)
        events.sort(key=lambda e: e.created_at, reverse=True)
        return events

    def clear(self) -> None:
        self._by_id.clear()
