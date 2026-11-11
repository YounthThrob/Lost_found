from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class Handover:
    id: str
    claim_id: str
    proposer_id: str
    time_window: str
    public_place: str
    owner_confirmed_at: str | None
    claimant_confirmed_at: str | None
    status: str
    created_at: str


class InMemoryHandoverStore:
    """内存交接安排存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, Handover] = {}

    def save(self, handover: Handover) -> None:
        self._by_id[handover.id] = handover

    def find_by_id(self, handover_id: str) -> Handover | None:
        return self._by_id.get(handover_id)

    def find_by_claim_id(self, claim_id: str) -> Handover | None:
        for h in self._by_id.values():
            if h.claim_id == claim_id:
                return h
        return None

    def clear(self) -> None:
        self._by_id.clear()
