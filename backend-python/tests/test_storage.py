import pytest

from app.shared.errors import BusinessError
from app.storage import LocalFileStorage


def test_put_then_get_roundtrip(tmp_path):
    storage = LocalFileStorage(str(tmp_path))
    storage.put("abc123", b"\x01\x02\x03", "image/jpeg")
    assert storage.get("abc123") == b"\x01\x02\x03"


def test_delete_removes_content(tmp_path):
    storage = LocalFileStorage(str(tmp_path))
    storage.put("abc123", b"\x01", "image/jpeg")
    storage.delete("abc123")
    assert storage.get("abc123") is None


def test_invalid_key_is_rejected(tmp_path):
    storage = LocalFileStorage(str(tmp_path))
    with pytest.raises(BusinessError):
        storage.put("../evil", b"\x01", "image/jpeg")
    with pytest.raises(BusinessError):
        storage.put("has space", b"\x01", "image/jpeg")
