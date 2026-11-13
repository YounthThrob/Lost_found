from dataclasses import dataclass, replace
from datetime import datetime, timezone
from uuid import uuid4


def now_utc() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class School:
    id: str
    name: str
    aliases: tuple[str, ...]
    source: str
    enabled: bool
    created_at: str


@dataclass
class Campus:
    id: str
    school_id: str
    name: str
    status: str  # PENDING | ACTIVE | REJECTED
    created_at: str


# 学校种子（教育部《全国普通高等学校名单》代表性条目 + 试点学校）。
# (school_id, name, aliases, [(campus_name, status), ...])
_SEED = [
    (
        "018c0000-0000-7000-8000-000000000001",
        "南方大学",
        ("南方大学", "南方大"),
        [("东校区", "ACTIVE"), ("西校区", "PENDING")],
    ),
    (
        "018c0000-0000-7000-8000-000000000002",
        "北京大学",
        ("北京大学", "北大"),
        [("燕园校区", "ACTIVE")],
    ),
    (
        "018c0000-0000-7000-8000-000000000003",
        "清华大学",
        ("清华大学", "清华"),
        [("清华园校区", "ACTIVE")],
    ),
    (
        "018c0000-0000-7000-8000-000000000004",
        "浙江大学",
        ("浙江大学", "浙大"),
        [("紫金港校区", "ACTIVE")],
    ),
]


class InMemoryDirectoryStore:
    """内存学校/校区目录存储。walking skeleton 阶段使用内存，后续以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._schools: dict[str, School] = {}
        self._campuses: dict[str, Campus] = {}
        self._seed()

    def list_schools(self, keyword: str | None) -> list[School]:
        schools = [s for s in self._schools.values() if s.enabled]
        if keyword and keyword.strip():
            kw = keyword.strip().lower()
            schools = [
                s for s in schools
                if kw in s.name.lower() or any(kw in a.lower() for a in s.aliases)
            ]
        schools.sort(key=lambda s: s.name)
        return schools

    def find_school(self, school_id: str) -> School | None:
        school = self._schools.get(school_id)
        if school is None or not school.enabled:
            return None
        return school

    def list_campuses(self, school_id: str, active_only: bool = True) -> list[Campus]:
        campuses = [c for c in self._campuses.values() if c.school_id == school_id]
        if active_only:
            campuses = [c for c in campuses if c.status == "ACTIVE"]
        campuses.sort(key=lambda c: c.name)
        return campuses

    def find_campus(self, campus_id: str) -> Campus | None:
        return self._campuses.get(campus_id)

    def create_campus(self, school_id: str, name: str) -> Campus:
        campus = Campus(
            id=str(uuid4()),
            school_id=school_id,
            name=name,
            status="PENDING",
            created_at=now_utc(),
        )
        self._campuses[campus.id] = campus
        return campus

    def set_campus_status(self, campus_id: str, status: str) -> Campus | None:
        campus = self._campuses.get(campus_id)
        if campus is None:
            return None
        updated = replace(campus, status=status)
        self._campuses[campus_id] = updated
        return updated

    def _seed(self) -> None:
        for school_id, name, aliases, campuses in _SEED:
            self._schools[school_id] = School(
                id=school_id,
                name=name,
                aliases=aliases,
                source="SEED",
                enabled=True,
                created_at=now_utc(),
            )
            for campus_name, status in campuses:
                campus = Campus(
                    id=str(uuid4()),
                    school_id=school_id,
                    name=campus_name,
                    status=status,
                    created_at=now_utc(),
                )
                self._campuses[campus.id] = campus
