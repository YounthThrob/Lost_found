from uuid import uuid4

from app.moderation.store import InMemoryReportStore, Report, now_utc
from app.shared.errors import VALIDATION_FAILED, BusinessError

_store = InMemoryReportStore()

# 必须与 contracts/moderation-rules.yaml 的 banned_keywords 保持一致。
BANNED_KEYWORDS = ["转账", "刷单", "诈骗"]

MAX_REASON = 100
MAX_DESCRIPTION = 500
MAX_ID = 100
MAX_PAGE_SIZE = 100


def is_rejected(title: str | None, description: str | None) -> bool:
    """对标题与公开描述做大小写不敏感包含匹配，命中任一违禁词返回 True。"""
    t = (title or "").lower()
    d = (description or "").lower()
    return any(kw in t or kw in d for kw in BANNED_KEYWORDS)


def create_report(reporter_id: str, *, target_type: str | None, target_id: str | None,
                  reason: str | None, description: str | None) -> Report:
    tt = target_type.strip() if target_type else None
    if tt not in ("POST", "USER"):
        raise BusinessError(VALIDATION_FAILED)
    _require_text(target_id, MAX_ID)
    _require_text(reason, MAX_REASON)
    desc = _normalize(description)
    if desc is not None and len(desc) > MAX_DESCRIPTION:
        raise BusinessError(VALIDATION_FAILED)
    report = Report(
        id=str(uuid4()),
        reporter_id=reporter_id,
        target_type=tt,
        target_id=target_id.strip(),
        reason=reason.strip(),
        description=desc,
        status="SUBMITTED",
        created_at=now_utc(),
    )
    _store.save(report)
    return report


def list_reports(reporter_id: str, page: int, page_size: int) -> dict:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
    reports = _store.list_by_reporter(reporter_id)
    total = len(reports)
    start = (page - 1) * page_size
    items = reports[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def _require_text(value: str | None, max_len: int) -> None:
    if value is None or not value.strip() or len(value.strip()) > max_len:
        raise BusinessError(VALIDATION_FAILED)


def _normalize(value: str | None) -> str | None:
    if value is None or not value.strip():
        return None
    return value.strip()
