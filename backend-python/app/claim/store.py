from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class Claim:
    id: str
    post_id: str
    applicant_id: str
    statement: str
    status: str
    version: int
    created_at: str
    updated_at: str


class InMemoryClaimStore:
    """内存认领申请存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, Claim] = {}

    def save(self, claim: Claim) -> None:
        self._by_id[claim.id] = claim

    def find_by_id(self, claim_id: str) -> Claim | None:
        return self._by_id.get(claim_id)

    def list_by_post(self, post_id: str) -> list[Claim]:
        claims = [c for c in self._by_id.values() if c.post_id == post_id]
        claims.sort(key=lambda c: c.created_at, reverse=True)
        return claims

    def list_by_applicant(self, applicant_id: str) -> list[Claim]:
        claims = [c for c in self._by_id.values() if c.applicant_id == applicant_id]
        claims.sort(key=lambda c: c.id)
        claims.sort(key=lambda c: c.created_at, reverse=True)
        return claims

    def find_active(self, applicant_id: str, post_id: str) -> Claim | None:
        for c in self._by_id.values():
            if c.applicant_id == applicant_id and c.post_id == post_id and _is_active(c.status):
                return c
        return None

    def exists_accepted_on_post(self, post_id: str) -> bool:
        return any(c.post_id == post_id and c.status == "ACCEPTED" for c in self._by_id.values())

    def has_other_active(self, post_id: str, exclude_claim_id: str) -> bool:
        return any(
            c.post_id == post_id and c.id != exclude_claim_id and _is_active(c.status)
            for c in self._by_id.values()
        )

    def clear(self) -> None:
        self._by_id.clear()


def _is_active(status: str) -> bool:
    return status in ("SUBMITTED", "NEEDS_MORE_INFO", "ACCEPTED")
