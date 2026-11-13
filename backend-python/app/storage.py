import re
from pathlib import Path
from typing import Protocol

from app.config import STORAGE_LOCAL_DIR
from app.shared.errors import VALIDATION_FAILED, BusinessError

_KEY_RE = re.compile(r"^[0-9a-f-]+$")


class Storage(Protocol):
    """图片二进制存储抽象。MVP 落地为本地文件系统，后续可替换为对象存储。"""

    def put(self, key: str, content: bytes, content_type: str) -> None: ...
    def get(self, key: str) -> bytes | None: ...
    def delete(self, key: str) -> None: ...


class LocalFileStorage:
    """本地文件系统存储。写 STORAGE_LOCAL_DIR（默认 ./data/media）。key 仅允许 [0-9a-f-]。"""

    def __init__(self, base_dir: str = STORAGE_LOCAL_DIR):
        self._base_dir = Path(base_dir).resolve()

    def put(self, key: str, content: bytes, content_type: str) -> None:
        target = self._resolve(key)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

    def get(self, key: str) -> bytes | None:
        target = self._resolve(key)
        if not target.exists():
            return None
        return target.read_bytes()

    def delete(self, key: str) -> None:
        self._resolve(key).unlink(missing_ok=True)

    def _resolve(self, key: str) -> Path:
        if not key or not _KEY_RE.match(key):
            raise BusinessError(VALIDATION_FAILED)
        return self._base_dir / key
