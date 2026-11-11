import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.claim import service as claim_service
from app.claim.store import InMemoryClaimStore
from app.communication import service as communication_service
from app.communication.store import InMemoryBlockStore, InMemoryMessageStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app
from app.moderation import service as moderation_service
from app.moderation.store import InMemoryReportStore

client = TestClient(app)

POST_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失一张校园卡",
    "school": "南方大学",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    claim_service._store = InMemoryClaimStore()
    communication_service._block_store = InMemoryBlockStore()
    communication_service._message_store = InMemoryMessageStore()
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


def _me_id(token: str) -> str:
    return client.get("/api/v1/me", headers=_headers(token)).json()["data"]["id"]


def test_claim_rate_limit_returns_429():
    token = _confirmed_token("valid-code-4")
    for _ in range(20):
        resp = client.post("/api/v1/posts/dummy-post/claims", headers=_headers(token),
                           json={"statement": "x"})
        assert resp.status_code == 404
    resp = client.post("/api/v1/posts/dummy-post/claims", headers=_headers(token),
                       json={"statement": "x"})
    assert resp.status_code == 429
    assert resp.json()["error"]["code"] == "CLAIM_RATE_LIMITED"


def test_post_rate_limit_returns_429():
    token = _confirmed_token("valid-code-4")
    for _ in range(20):
        resp = client.post("/api/v1/posts", headers=_headers(token), json=POST_BODY)
        assert resp.status_code == 200
    resp = client.post("/api/v1/posts", headers=_headers(token), json=POST_BODY)
    assert resp.status_code == 429
    assert resp.json()["error"]["code"] == "RATE_LIMITED"


def test_message_rate_limit_returns_429():
    token = _confirmed_token("valid-code-4")
    for _ in range(30):
        resp = client.post("/api/v1/conversations/dummy/messages", headers=_headers(token),
                           json={"type": "TEXT", "content": "hi"})
        assert resp.status_code == 404
    resp = client.post("/api/v1/conversations/dummy/messages", headers=_headers(token),
                       json={"type": "TEXT", "content": "hi"})
    assert resp.status_code == 429
    assert resp.json()["error"]["code"] == "RATE_LIMITED"


def test_report_rate_limit_returns_429():
    token = _confirmed_token("valid-code-4")
    for _ in range(20):
        resp = client.post("/api/v1/reports", headers=_headers(token),
                           json={"targetType": "POST", "targetId": "post-x", "reason": "疑似诈骗"})
        assert resp.status_code == 200
    resp = client.post("/api/v1/reports", headers=_headers(token),
                       json={"targetType": "POST", "targetId": "post-x", "reason": "疑似诈骗"})
    assert resp.status_code == 429
    assert resp.json()["error"]["code"] == "RATE_LIMITED"


def test_different_operations_count_independently():
    token = _confirmed_token("valid-code-4")
    for _ in range(20):
        client.post("/api/v1/posts/dummy-post/claims", headers=_headers(token),
                    json={"statement": "x"})
    resp = client.post("/api/v1/posts", headers=_headers(token), json=POST_BODY)
    assert resp.status_code == 200


def test_restricted_account_is_rejected():
    token = _confirmed_token("valid-code")
    account_service._store.restrict(_me_id(token))
    resp = client.post("/api/v1/posts", headers=_headers(token), json=POST_BODY)
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "ACCOUNT_RESTRICTED"


def test_normal_account_is_unaffected():
    token = _confirmed_token("valid-code")
    resp = client.post("/api/v1/posts", headers=_headers(token), json=POST_BODY)
    assert resp.status_code == 200


def test_error_response_does_not_leak_wind_control_details():
    token = _confirmed_token("valid-code-4")
    for _ in range(20):
        client.post("/api/v1/posts/dummy-post/claims", headers=_headers(token),
                    json={"statement": "x"})
    resp = client.post("/api/v1/posts/dummy-post/claims", headers=_headers(token),
                       json={"statement": "x"})
    assert resp.status_code == 429
    assert resp.json()["error"]["code"] == "CLAIM_RATE_LIMITED"
    assert resp.json()["error"]["message"] == "认领申请过于频繁，请稍后再试"
    assert resp.json()["error"]["details"] == {}
    assert "window" not in resp.text
