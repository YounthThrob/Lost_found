import pytest
from fastapi.testclient import TestClient

from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.claim import service as claim_service
from app.claim.store import InMemoryClaimStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app

client = TestClient(app)

LOST_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失校园卡",
    "school": "南方大学",
}
FOUND_BODY = {
    "type": "FOUND",
    "category": "校园卡",
    "title": "拾到校园卡",
    "publicDescription": "在食堂拾到一张校园卡",
    "school": "南方大学",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    claim_service._store = InMemoryClaimStore()
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
    return client.post("/api/v1/posts", headers=_headers(token), json=LOST_BODY).json()["data"]["id"]


def _create_published_found(token: str) -> str:
    post_id = client.post("/api/v1/posts", headers=_headers(token), json=FOUND_BODY).json()["data"]["id"]
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    return post_id


def test_my_posts_includes_draft_and_isolates():
    owner = _confirmed_token("valid-code")
    other = _confirmed_token("valid-code-2")
    _create_draft(owner)

    resp = client.get("/api/v1/me/posts", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["status"] == "DRAFT"

    resp = client.get("/api/v1/me/posts", headers=_headers(other))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_my_claims_isolates():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_found(owner)

    client.post(
        f"/api/v1/posts/{post_id}/claims",
        headers=_headers(applicant),
        json={"statement": "我的校园卡"},
    )

    resp = client.get("/api/v1/me/claims", headers=_headers(applicant))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1

    resp = client.get("/api/v1/me/claims", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 0
