import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app
from app.moderation import service as moderation_service
from app.moderation.store import InMemoryReportStore

client = TestClient(app)

CLEAN_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失一张校园卡",
    "school": "南方大学",
}

BANNED_BODY = {
    "type": "LOST",
    "category": "其他",
    "title": "高价回收",
    "publicDescription": "要求先转账到指定账户",
    "school": "南方大学",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    moderation_service._store = InMemoryReportStore()
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
    return client.post("/api/v1/posts", headers=_headers(token), json=body).json()["data"]["id"]


def test_publish_banned_keyword_returns_rejected():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, BANNED_BODY)
    resp = client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "REJECTED"


def test_publish_clean_returns_published():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, CLEAN_BODY)
    resp = client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PUBLISHED"


def test_rejected_post_hidden_from_search():
    token = _confirmed_token("valid-code")
    post_id = _create_draft(token, BANNED_BODY)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    resp = client.get("/api/v1/posts", headers=_headers(token))
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_rejected_post_visible_to_author_only():
    token1 = _confirmed_token("valid-code")
    token2 = _confirmed_token("valid-code-2")
    post_id = _create_draft(token1, BANNED_BODY)
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token1))

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token1))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "REJECTED"

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(token2))
    assert resp.status_code == 404


def test_create_report_returns_submitted():
    token = _confirmed_token("valid-code")
    resp = client.post("/api/v1/reports", headers=_headers(token), json={
        "targetType": "POST", "targetId": "post-abc123", "reason": "疑似诈骗",
    })
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["status"] == "SUBMITTED"
    assert data["id"]


def test_create_report_invalid_type_returns_422():
    token = _confirmed_token("valid-code")
    resp = client.post("/api/v1/reports", headers=_headers(token), json={
        "targetType": "OTHER", "targetId": "post-abc123", "reason": "其他",
    })
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_create_report_empty_reason_returns_422():
    token = _confirmed_token("valid-code")
    resp = client.post("/api/v1/reports", headers=_headers(token), json={
        "targetType": "POST", "targetId": "post-abc123", "reason": "",
    })
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_list_my_reports_returns_own_only():
    token1 = _confirmed_token("valid-code")
    token2 = _confirmed_token("valid-code-2")

    client.post("/api/v1/reports", headers=_headers(token1), json={
        "targetType": "POST", "targetId": "post-abc123", "reason": "疑似诈骗",
    })

    resp = client.get("/api/v1/reports/me", headers=_headers(token1))
    assert resp.json()["data"]["pagination"]["total"] == 1

    resp = client.get("/api/v1/reports/me", headers=_headers(token2))
    assert resp.json()["data"]["pagination"]["total"] == 0
