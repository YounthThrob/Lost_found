from app.directory.store import Campus, InMemoryDirectoryStore, School
from app.shared.errors import NOT_FOUND, VALIDATION_FAILED, BusinessError

_store = InMemoryDirectoryStore()

MAX_NAME = 100
MAX_PAGE_SIZE = 100


def search_schools(keyword: str | None, page: int, page_size: int) -> dict:
    _validate_page(page, page_size)
    schools = _store.list_schools(keyword)
    total = len(schools)
    start = (page - 1) * page_size
    items = schools[start:start + page_size]
    has_more = start + page_size < total
    return {
        "items": items,
        "pagination": {"page": page, "pageSize": page_size, "total": total, "hasMore": has_more},
    }


def list_campuses(school_id: str) -> list[Campus]:
    _require_school(school_id)
    return _store.list_campuses(school_id, active_only=True)


def request_campus(school_id: str, name: str | None) -> Campus:
    _require_school(school_id)
    normalized = _require_name(name)
    existing = _store.list_campuses(school_id, active_only=False)
    if any(c.name == normalized for c in existing):
        raise BusinessError(VALIDATION_FAILED)
    return _store.create_campus(school_id, normalized)


def review_campus(campus_id: str, decision: str | None) -> Campus:
    normalized = (decision or "").strip().upper()
    if normalized not in ("APPROVE", "REJECT"):
        raise BusinessError(VALIDATION_FAILED)
    status = "ACTIVE" if normalized == "APPROVE" else "REJECTED"
    campus = _store.set_campus_status(campus_id, status)
    if campus is None:
        raise BusinessError(NOT_FOUND)
    return campus


# ---- 供 account / listing 复用的只读查询（返回 None 而非抛错，由调用方决定错误码）----


def find_school(school_id: str | None) -> School | None:
    if not school_id:
        return None
    return _store.find_school(school_id)


def find_active_campus(school_id: str, campus_id: str) -> Campus | None:
    campus = _store.find_campus(campus_id)
    if campus is None or campus.school_id != school_id or campus.status != "ACTIVE":
        return None
    return campus


def find_school_name(school_id: str | None) -> str | None:
    school = find_school(school_id)
    return school.name if school else None


def find_campus_name(campus_id: str | None) -> str | None:
    if not campus_id:
        return None
    campus = _store.find_campus(campus_id)
    return campus.name if campus else None


def _require_school(school_id: str) -> School:
    school = _store.find_school(school_id)
    if school is None:
        raise BusinessError(NOT_FOUND)
    return school


def _require_name(name: str | None) -> str:
    if name is None or not name.strip() or len(name.strip()) > MAX_NAME:
        raise BusinessError(VALIDATION_FAILED)
    return name.strip()


def _validate_page(page: int, page_size: int) -> None:
    if page < 1 or page_size < 1 or page_size > MAX_PAGE_SIZE:
        raise BusinessError(VALIDATION_FAILED)
