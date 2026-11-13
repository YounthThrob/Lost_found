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

client = TestClient(app)

FOUND_BODY = {
    "type": "FOUND",
    "category": "校园卡",
    "title": "拾到校园卡",
    "publicDescription": "在食堂拾到一张校园卡",
    "schoolId": "018c0000-0000-7000-8000-000000000001",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    claim_service._store = InMemoryClaimStore()
    communication_service._block_store = InMemoryBlockStore()
    communication_service._message_store = InMemoryMessageStore()
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


def _create_published_post(token: str, body: dict) -> str:
    post_id = client.post("/api/v1/posts", headers=_headers(token), json=body).json()["data"]["id"]
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    return post_id


def _accepted_claim(owner_token: str, applicant_token: str) -> str:
    post_id = _create_published_post(owner_token, FOUND_BODY)
    claim_id = client.post(f"/api/v1/posts/{post_id}/claims", headers=_headers(applicant_token),
                           json={"statement": "我的校园卡"}).json()["data"]["id"]
    client.post(f"/api/v1/claims/{claim_id}/accept", headers=_headers(owner_token))
    return claim_id


def test_block_and_unblock_lifecycle():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    applicant_id = _me_id(applicant)
    owner_id = _me_id(owner)

    resp = client.post("/api/v1/blocks", headers=_headers(owner), json={"blockedUserId": applicant_id})
    assert resp.status_code == 200
    assert resp.json()["data"]["blockedUserId"] == applicant_id

    resp = client.post("/api/v1/blocks", headers=_headers(owner), json={"blockedUserId": applicant_id})
    assert resp.status_code == 200

    resp = client.post("/api/v1/blocks", headers=_headers(owner), json={"blockedUserId": owner_id})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"

    resp = client.post("/api/v1/blocks", headers=_headers(owner), json={"blockedUserId": "nonexistent"})
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"

    resp = client.get("/api/v1/me/blocks", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1

    resp = client.delete(f"/api/v1/blocks/{applicant_id}", headers=_headers(owner))
    assert resp.status_code == 200

    resp = client.get("/api/v1/me/blocks", headers=_headers(owner))
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_message_send_list_and_blocked():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    applicant_id = _me_id(applicant)
    claim_id = _accepted_claim(owner, applicant)

    resp = client.post(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(applicant),
                       json={"type": "TEXT", "content": "明天下午3点方便吗"})
    assert resp.status_code == 200
    assert resp.json()["data"]["type"] == "TEXT"
    assert resp.json()["data"]["senderId"] == applicant_id

    resp = client.get(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1

    client.post("/api/v1/blocks", headers=_headers(owner), json={"blockedUserId": applicant_id})
    resp = client.post(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(applicant),
                       json={"type": "TEXT", "content": "还在吗"})
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "BLOCKED"

    client.delete(f"/api/v1/blocks/{applicant_id}", headers=_headers(owner))
    resp = client.post(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(applicant),
                       json={"type": "SUGGEST_TIME", "content": "明天下午3点"})
    assert resp.status_code == 200
    assert resp.json()["data"]["type"] == "SUGGEST_TIME"


def test_message_authorization_and_validation():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    third = _confirmed_token("valid-code-3")
    claim_id = _accepted_claim(owner, applicant)

    resp = client.get(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(third))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"

    post_id = _create_published_post(owner, FOUND_BODY)
    unaccepted_claim = client.post(f"/api/v1/posts/{post_id}/claims", headers=_headers(third),
                                   json={"statement": "我的"}).json()["data"]["id"]
    resp = client.post(f"/api/v1/conversations/{unaccepted_claim}/messages", headers=_headers(third),
                       json={"type": "TEXT", "content": "hi"})
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"

    resp = client.post(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(applicant),
                       json={"type": "INVALID", "content": "x"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"

    resp = client.post(f"/api/v1/conversations/{claim_id}/messages", headers=_headers(applicant),
                       json={"type": "TEXT", "content": ""})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"
