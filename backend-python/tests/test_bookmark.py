import pytest
from fastapi.testclient import TestClient

from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.bookmark import service as bookmark_service
from app.bookmark.store import InMemoryBookmarkStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app

client = TestClient(app)

LOST_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失校园卡",
    "schoolId": "018c0000-0000-7000-8000-000000000001",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    bookmark_service._store = InMemoryBookmarkStore()
    yield


def _login_token(code: str) -> str:
    return client.post("/api/v1/session/wechat", json={"code": code}).json()["data"]["token"]


def _confirmed_token(code: str) -> str:
    token = _login_token(code)
    client.post("/api/v1/me/agreement", headers=_headers(token))
    return token


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def _create_published_post(token: str) -> str:
    post_id = client.post("/api/v1/posts", headers=_headers(token), json=LOST_BODY).json()["data"]["id"]
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    return post_id


def test_bookmark_published_returns_post():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    post_id = _create_published_post(owner)

    resp = client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))
    assert resp.status_code == 200
    assert resp.json()["data"]["postId"] == post_id
    assert resp.json()["data"]["post"]["id"] == post_id
    assert resp.json()["data"]["post"]["status"] == "PUBLISHED"


def test_bookmark_duplicate_is_idempotent():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    post_id = _create_published_post(owner)

    client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))
    client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))

    resp = client.get("/api/v1/me/bookmarks", headers=_headers(user))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1


def test_bookmark_nonvisible_returns_404():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    post_id = _create_published_post(owner)
    client.post(f"/api/v1/posts/{post_id}/close", headers=_headers(owner))

    resp = client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_bookmark_nonexistent_returns_404():
    user = _login_token("valid-code-2")

    resp = client.post("/api/v1/posts/nonexistent-id/bookmark", headers=_headers(user))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_list_returns_bookmark_with_status():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    post_id = _create_published_post(owner)
    client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))

    resp = client.get("/api/v1/me/bookmarks", headers=_headers(user))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["postId"] == post_id
    assert resp.json()["data"]["items"][0]["post"]["status"] == "PUBLISHED"


def test_list_is_isolated_between_users():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    other = _login_token("valid-code-3")
    post_id = _create_published_post(owner)
    client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))

    resp = client.get("/api/v1/me/bookmarks", headers=_headers(other))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_unbookmark_removes():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    post_id = _create_published_post(owner)
    client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))

    resp = client.delete(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))
    assert resp.status_code == 200

    resp = client.get("/api/v1/me/bookmarks", headers=_headers(user))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_unbookmark_idempotent():
    user = _login_token("valid-code-2")

    resp = client.delete("/api/v1/posts/some-post/bookmark", headers=_headers(user))
    assert resp.status_code == 200


def test_bookmark_survives_post_close_with_status():
    owner = _confirmed_token("valid-code")
    user = _login_token("valid-code-2")
    post_id = _create_published_post(owner)
    client.post(f"/api/v1/posts/{post_id}/bookmark", headers=_headers(user))
    client.post(f"/api/v1/posts/{post_id}/close", headers=_headers(owner))

    resp = client.get("/api/v1/me/bookmarks", headers=_headers(user))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["post"]["status"] == "CLOSED"
