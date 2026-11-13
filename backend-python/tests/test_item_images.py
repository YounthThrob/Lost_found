import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemImageStore, InMemoryItemPostStore
from app.main import app

client = TestClient(app)

CREATE_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失一张校园卡",
    "schoolId": "018c0000-0000-7000-8000-000000000001",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    listing_service._image_store = InMemoryItemImageStore()
    rate_limiter.clear()
    yield


def _login_token(code: str) -> str:
    return client.post("/api/v1/session/wechat", json={"code": code}).json()["data"]["token"]


def _confirmed_token(code: str) -> str:
    token = _login_token(code)
    client.post("/api/v1/me/agreement", headers=_headers(token))
    return token


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def _create_draft(token: str) -> str:
    return client.post("/api/v1/posts", headers=_headers(token), json=CREATE_BODY).json()["data"]["id"]


def _upload(token: str, post_id: str, name: str = "card.jpg", content_type: str = "image/jpeg",
            content: bytes = b"\x01") -> str:
    resp = client.post(f"/api/v1/posts/{post_id}/images", headers=_headers(token),
                       files={"file": (name, content, content_type)})
    return resp.json()["data"]["id"]


def test_upload_success_and_draft_detail_includes_image():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token)

    resp = client.post(f"/api/v1/posts/{post_id}/images", headers=_headers(token),
                       files={"file": ("card.jpg", b"\x01\x02", "image/jpeg")})
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["id"]
    assert data["url"].startswith("/media/")
    assert data["sortOrder"] == 0

    detail = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token))
    assert len(detail.json()["data"]["images"]) == 1


def test_upload_bad_type_returns_validation_failed():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token)

    resp = client.post(f"/api/v1/posts/{post_id}/images", headers=_headers(token),
                       files={"file": ("card.gif", b"\x01", "image/gif")})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_upload_oversize_returns_validation_failed():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token)

    resp = client.post(f"/api/v1/posts/{post_id}/images", headers=_headers(token),
                       files={"file": ("big.jpg", b"x" * (10 * 1024 * 1024 + 1), "image/jpeg")})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_upload_by_non_author_returns_not_found():
    owner = _confirmed_token("valid-code")
    other = _confirmed_token("valid-code-2")
    post_id = _create_draft(owner)

    resp = client.post(f"/api/v1/posts/{post_id}/images", headers=_headers(other),
                       files={"file": ("card.jpg", b"\x01", "image/jpeg")})
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_upload_to_published_returns_invalid_state_transition():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))

    resp = client.post(f"/api/v1/posts/{post_id}/images", headers=_headers(token),
                       files={"file": ("card.jpg", b"\x01", "image/jpeg")})
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_draft_hidden_and_public_visible_with_images():
    owner = _confirmed_token("valid-code")
    other = _confirmed_token("valid-code-2")
    post_id = _create_draft(owner)
    _upload(owner, post_id)

    # 草稿对非作者不可见
    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(other))
    assert resp.status_code == 404

    # 发布后公开，图片对所有人可见
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(owner))
    detail = client.get(f"/api/v1/posts/{post_id}", headers=_headers(other))
    assert detail.status_code == 200
    assert len(detail.json()["data"]["images"]) == 1

    search = client.get("/api/v1/posts", headers=_headers(other))
    items = search.json()["data"]["items"]
    assert len(items[0]["images"]) == 1


def test_delete_image_success():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token)
    image_id = _upload(token, post_id)

    resp = client.delete(f"/api/v1/posts/{post_id}/images/{image_id}", headers=_headers(token))
    assert resp.status_code == 200

    detail = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token))
    assert len(detail.json()["data"]["images"]) == 0


def test_delete_by_non_author_returns_not_found():
    owner = _confirmed_token("valid-code")
    other = _confirmed_token("valid-code-2")
    post_id = _create_draft(owner)
    image_id = _upload(owner, post_id)

    resp = client.delete(f"/api/v1/posts/{post_id}/images/{image_id}", headers=_headers(other))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"
