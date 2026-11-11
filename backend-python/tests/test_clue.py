import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.clue import service as clue_service
from app.clue.store import InMemoryClueStore
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
    clue_service._store = InMemoryClueStore()
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


def _create_published_post(token: str, body: dict) -> str:
    post_id = client.post("/api/v1/posts", headers=_headers(token), json=body).json()["data"]["id"]
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    return post_id


def _submit_clue(token: str, post_id: str, content: str):
    return client.post(f"/api/v1/posts/{post_id}/clues", headers=_headers(token),
                       json={"content": content})


def test_submit_clue_on_lost_returns_submitted():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)

    resp = _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡")
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "SUBMITTED"
    assert resp.json()["data"]["providerId"]
    assert resp.json()["data"]["content"] == "我在图书馆门口看到过这张卡"


def test_submit_clue_without_agreement_returns_403():
    owner = _confirmed_token("valid-code")
    provider = _login_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)

    resp = _submit_clue(provider, post_id, "一条线索")
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "AGREEMENT_NOT_ACCEPTED"


def test_submit_clue_on_found_returns_409():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)

    resp = _submit_clue(provider, post_id, "线索不该给招领")
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "CLUE_NOT_ALLOWED"


def test_submit_clue_on_closed_returns_404():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    client.post(f"/api/v1/posts/{post_id}/close", headers=_headers(owner))

    resp = _submit_clue(provider, post_id, "线索不该给已关闭信息")
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_submit_clue_banned_returns_rejected():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)

    resp = _submit_clue(provider, post_id, "先转账给我再告诉你")
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "REJECTED"


def test_submit_empty_clue_returns_422():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)

    resp = _submit_clue(provider, post_id, "")
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_owner_lists_pending_clues_and_non_owner_cannot():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡")

    resp = client.get(f"/api/v1/posts/{post_id}/clues", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["status"] == "SUBMITTED"

    resp = client.get(f"/api/v1/posts/{post_id}/clues", headers=_headers(provider))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_rejected_clue_not_in_pending_list():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    _submit_clue(provider, post_id, "先转账给我再告诉你")

    resp = client.get(f"/api/v1/posts/{post_id}/clues", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_decide_valid_returns_accepted():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    clue_id = _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/clues/{clue_id}/decide", headers=_headers(owner),
                       json={"decision": "VALID"})
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "ACCEPTED"


def test_decide_invalid_returns_closed():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    clue_id = _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/clues/{clue_id}/decide", headers=_headers(owner),
                       json={"decision": "INVALID"})
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "CLOSED"


def test_decide_again_returns_409():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    clue_id = _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡").json()["data"]["id"]
    client.post(f"/api/v1/clues/{clue_id}/decide", headers=_headers(owner), json={"decision": "VALID"})

    resp = client.post(f"/api/v1/clues/{clue_id}/decide", headers=_headers(owner),
                       json={"decision": "INVALID"})
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_decide_invalid_decision_returns_422():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    clue_id = _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/clues/{clue_id}/decide", headers=_headers(owner),
                       json={"decision": "MAYBE"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_non_owner_cannot_decide():
    owner = _confirmed_token("valid-code")
    provider = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)
    clue_id = _submit_clue(provider, post_id, "我在图书馆门口看到过这张卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/clues/{clue_id}/decide", headers=_headers(provider),
                       json={"decision": "VALID"})
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"
