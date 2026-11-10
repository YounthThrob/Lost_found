from dataclasses import dataclass
from datetime import datetime, timezone


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class Report:
    id: str
    reporter_id: str
    target_type: str
    target_id: str
    reason: str
    description: str | None
    status: str
    created_at: str


class InMemoryReportStore:
    """内存举报存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_id: dict[str, Report] = {}

    def save(self, report: Report) -> None:
        self._by_id[report.id] = report

    def list_by_reporter(self, reporter_id: str) -> list[Report]:
        reports = [r for r in self._by_id.values() if r.reporter_id == reporter_id]
        reports.sort(key=lambda r: r.created_at, reverse=True)
        return reports

    def clear(self) -> None:
        self._by_id.clear()
