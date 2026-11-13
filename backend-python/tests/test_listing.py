import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app

client = TestClient(app)

CREATE_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失一张校园卡",
    "schoolId": "018c0000-0000-7000-8000-000000000001",
    "area": "图书馆",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
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


def _create_draft(token: str, body: dict) -> str:
    resp = client.post("/api/v1/posts", headers=_headers(token), json=body)
    return resp.json()["data"]["id"]


def test_create_draft_returns_draft_status():
    token = _confirmed_token("valid-code")
    resp = client.post("/api/v1/posts", headers=_headers(token), json=CREATE_BODY)
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["status"] == "DRAFT"
    assert data["type"] == "LOST"
    assert data["id"]


def test_create_without_agreement_returns_403():
    token = _login_token("valid-code")
    resp = client.post("/api/v1/posts", headers=_headers(token), json=CREATE_BODY)
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "AGREEMENT_NOT_ACCEPTED"


def test_create_empty_title_returns_422():
    token = _confirmed_token("valid-code")
    body = {**CREATE_BODY, "title": ""}
    resp = client.post("/api/v1/posts", headers=_headers(token), json=body)
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_create_invalid_type_returns_422():
    token = _confirmed_token("valid-code")
    body = {**CREATE_BODY, "type": "OTHER"}
    resp = client.post("/api/v1/posts", headers=_headers(token), json=body)
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_edit_draft_success():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CREATE_BODY)

    resp = client.patch(f"/api/v1/posts/{post_id}", headers=_headers(token), json={
        "type": "FOUND", "category": "雨伞", "title": "拾到雨伞",
        "publicDescription": "在食堂拾到一把雨伞", "schoolId": "018c0000-0000-7000-8000-000000000001",
    })
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["type"] == "FOUND"
    assert data["title"] == "拾到雨伞"
    assert data["status"] == "DRAFT"


def test_edit_published_returns_409():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CREATE_BODY)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))

    resp = client.patch(f"/api/v1/posts/{post_id}", headers=_headers(token), json=CREATE_BODY)
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_edit_others_returns_404():
    token1 = _confirmed_token("valid-code")
    token2 = _confirmed_token("valid-code-2")
    post_id = _create_draft(token1, CREATE_BODY)

    resp = client.patch(f"/api/v1/posts/{post_id}", headers=_headers(token2), json=CREATE_BODY)
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_publish_then_detail_returns_published():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CREATE_BODY)

    resp = client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PUBLISHED"

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PUBLISHED"


def test_publish_again_returns_409():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CREATE_BODY)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))

    resp = client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_close_draft_returns_409():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CREATE_BODY)

    resp = client.post(f"/api/v1/posts/{post_id}/close", headers=_headers(token))
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_close_then_search_excludes():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CREATE_BODY)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    resp = client.post(f"/api/v1/posts/{post_id}/close", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "CLOSED"

    resp = client.get("/api/v1/posts", headers=_headers(token))
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_search_only_published_and_filters():
    token = _confirmed_token("valid-code")
    _create_draft(token, CREATE_BODY)  # 草稿，不应出现

    found_body = {**CREATE_BODY, "type": "FOUND", "title": "拾到雨伞", "category": "雨伞"}
    found_id = _create_draft(token, found_body)
    client.post(f"/api/v1/posts/{found_id}/publish", headers=_headers(token))

    resp = client.get("/api/v1/posts", headers=_headers(token))
    data = resp.json()["data"]
    assert data["pagination"]["total"] == 1
    assert data["items"][0]["id"] == found_id
    assert data["items"][0]["type"] == "FOUND"

    resp = client.get("/api/v1/posts", params={"type": "FOUND"}, headers=_headers(token))
    assert resp.json()["data"]["pagination"]["total"] == 1

    resp = client.get("/api/v1/posts", params={"type": "LOST"}, headers=_headers(token))
    assert resp.json()["data"]["pagination"]["total"] == 0

    resp = client.get("/api/v1/posts", params={"keyword": "雨伞"}, headers=_headers(token))
    assert resp.json()["data"]["pagination"]["total"] == 1


def test_search_pagination():
    token = _confirmed_token("valid-code")
    for i in range(3):
        body = {**CREATE_BODY, "title": f"物品{i}"}
        post_id = _create_draft(token, body)
        client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))

    resp = client.get("/api/v1/posts", params={"page": 1, "pageSize": 2}, headers=_headers(token))
    pagination = resp.json()["data"]["pagination"]
    assert pagination["page"] == 1
    assert pagination["pageSize"] == 2
    assert pagination["total"] == 3
    assert pagination["hasMore"] is True
    assert len(resp.json()["data"]["items"]) == 2


def test_search_invalid_page_returns_422():
    token = _confirmed_token("valid-code")
    resp = client.get("/api/v1/posts", params={"page": 0}, headers=_headers(token))
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_detail_draft_only_author_visible():
    token1 = _confirmed_token("valid-code")
    token2 = _confirmed_token("valid-code-2")
    post_id = _create_draft(token1, CREATE_BODY)

    assert client.get(f"/api/v1/posts/{post_id}", headers=_headers(token1)).status_code == 200

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token2))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_detail_not_found_returns_404():
    token = _confirmed_token("valid-code")
    resp = client.get("/api/v1/posts/nonexistent-id", headers=_headers(token))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_published_detail_visible_to_other_user():
    token1 = _confirmed_token("valid-code")
    token2 = _confirmed_token("valid-code-2")
    post_id = _create_draft(token1, CREATE_BODY)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token1))

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token2))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PUBLISHED"
