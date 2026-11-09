from dataclasses import dataclass
from uuid import uuid4


@dataclass
class UserAccount:
    id: str
    openid: str
    status: str


class InMemoryUserStore:
    """内存用户存储。walking skeleton 阶段使用内存，后续 change 以 SQLAlchemy 持久化替换。"""

    def __init__(self):
        self._by_openid: dict[str, UserAccount] = {}

    def find_by_openid(self, openid: str) -> UserAccount:
        user = self._by_openid.get(openid)
        if user is None:
            user = UserAccount(id=str(uuid4()), openid=openid, status="ACTIVE")
            self._by_openid[openid] = user
        return user

    def find_by_id(self, user_id: str) -> UserAccount | None:
        for user in self._by_openid.values():
            if user.id == user_id:
                return user
        return None
