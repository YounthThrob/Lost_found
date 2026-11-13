from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import uuid4


def _now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class UserAccount:
    id: str
    openid: str
    status: str
    agreement_accepted_at: str | None = None
    school: str | None = None
    campus: str | None = None
    role: str = "USER"
    verification_status: str = "SELF_DECLARED"
    verified_email: str | None = None
    email_verified_at: str | None = None

    @property
    def agreement_accepted(self) -> bool:
        return self.agreement_accepted_at is not None

    @property
    def is_operator(self) -> bool:
        return self.role == "OPERATOR"


class InMemoryUserStore:
    """内存用户存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_openid: dict[str, UserAccount] = {}

    def find_by_openid(self, openid: str, role: str = "USER") -> UserAccount:
        user = self._by_openid.get(openid)
        if user is None:
            user = UserAccount(id=str(uuid4()), openid=openid, status="ACTIVE", role=role)
            self._by_openid[openid] = user
        return user

    def find_by_id(self, user_id: str) -> UserAccount | None:
        for user in self._by_openid.values():
            if user.id == user_id:
                return user
        return None

    def confirm_agreement(self, user_id: str) -> UserAccount | None:
        user = self.find_by_id(user_id)
        if user is None:
            return None
        if user.agreement_accepted:
            return user
        updated = UserAccount(
            id=user.id,
            openid=user.openid,
            status=user.status,
            agreement_accepted_at=_now(),
            school=user.school,
            campus=user.campus,
            role=user.role,
            verification_status=user.verification_status,
            verified_email=user.verified_email,
            email_verified_at=user.email_verified_at,
        )
        self._by_openid[user.openid] = updated
        return updated

    def update_campus(self, user_id: str, school: str, campus: str | None) -> UserAccount | None:
        user = self.find_by_id(user_id)
        if user is None:
            return None
        updated = UserAccount(
            id=user.id,
            openid=user.openid,
            status=user.status,
            agreement_accepted_at=user.agreement_accepted_at,
            school=school,
            campus=campus,
            role=user.role,
            verification_status=user.verification_status,
            verified_email=user.verified_email,
            email_verified_at=user.email_verified_at,
        )
        self._by_openid[user.openid] = updated
        return updated

    def verify_email(self, user_id: str, email: str) -> UserAccount | None:
        user = self.find_by_id(user_id)
        if user is None:
            return None
        updated = UserAccount(
            id=user.id,
            openid=user.openid,
            status=user.status,
            agreement_accepted_at=user.agreement_accepted_at,
            school=user.school,
            campus=user.campus,
            role=user.role,
            verification_status="EMAIL_VERIFIED",
            verified_email=email,
            email_verified_at=_now(),
        )
        self._by_openid[user.openid] = updated
        return updated

    def restrict(self, user_id: str) -> UserAccount | None:
        return self._set_status(user_id, "RESTRICTED")

    def unrestrict(self, user_id: str) -> UserAccount | None:
        return self._set_status(user_id, "ACTIVE")

    def _set_status(self, user_id: str, status: str) -> UserAccount | None:
        user = self.find_by_id(user_id)
        if user is None:
            return None
        updated = UserAccount(
            id=user.id,
            openid=user.openid,
            status=status,
            agreement_accepted_at=user.agreement_accepted_at,
            school=user.school,
            campus=user.campus,
            role=user.role,
            verification_status=user.verification_status,
            verified_email=user.verified_email,
            email_verified_at=user.email_verified_at,
        )
        self._by_openid[user.openid] = updated
        return updated
