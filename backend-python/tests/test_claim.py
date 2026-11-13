import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.claim import service as claim_service
from app.claim.store import InMemoryClaimStore
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
    claim_service._store = InMemoryClaimStore()
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


def _submit_claim(token: str, post_id: str, statement: str):
    return client.post(f"/api/v1/posts/{post_id}/claims", headers=_headers(token),
                       json={"statement": statement})


def test_submit_claim_returns_submitted():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)

    resp = _submit_claim(applicant, post_id, "我的校园卡，学号尾号 0427")
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "SUBMITTED"
    assert resp.json()["data"]["id"]


def test_submit_claim_without_agreement_returns_403():
    owner = _confirmed_token("valid-code")
    applicant = _login_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)

    resp = _submit_claim(applicant, post_id, "我的校园卡")
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "AGREEMENT_NOT_ACCEPTED"


def test_submit_claim_on_lost_returns_409():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, LOST_BODY)

    resp = _submit_claim(applicant, post_id, "我的校园卡")
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "POST_NOT_CLAIMABLE"


def test_submit_claim_on_own_post_returns_409():
    owner = _confirmed_token("valid-code")
    post_id = _create_published_post(owner, FOUND_BODY)

    resp = _submit_claim(owner, post_id, "这是我自己的信息")
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "POST_NOT_CLAIMABLE"


def test_submit_duplicate_returns_409():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)

    assert _submit_claim(applicant, post_id, "我的校园卡").status_code == 200
    resp = _submit_claim(applicant, post_id, "再次申请")
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "CLAIM_DUPLICATE"


def test_submit_empty_statement_returns_422():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)

    resp = _submit_claim(applicant, post_id, "")
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_owner_lists_claims_and_non_owner_cannot():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)
    _submit_claim(applicant, post_id, "我的校园卡")

    resp = client.get(f"/api/v1/posts/{post_id}/claims", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["statement"] == "我的校园卡"

    resp = client.get(f"/api/v1/posts/{post_id}/claims", headers=_headers(applicant))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_request_info_returns_needs_more_info():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)
    claim_id = _submit_claim(applicant, post_id, "我的校园卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/claims/{claim_id}/request-info", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "NEEDS_MORE_INFO"


def test_request_info_on_accepted_returns_409():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)
    claim_id = _submit_claim(applicant, post_id, "我的校园卡").json()["data"]["id"]
    client.post(f"/api/v1/claims/{claim_id}/accept", headers=_headers(owner))

    resp = client.post(f"/api/v1/claims/{claim_id}/request-info", headers=_headers(owner))
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_accept_returns_accepted_and_post_pending_handover():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)
    claim_id = _submit_claim(applicant, post_id, "我的校园卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/claims/{claim_id}/accept", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "ACCEPTED"

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PENDING_HANDOVER"


def test_reject_returns_rejected_and_post_back_to_published():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)
    claim_id = _submit_claim(applicant, post_id, "我的校园卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/claims/{claim_id}/reject", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "REJECTED"

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(applicant))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PUBLISHED"


def test_accept_second_claim_returns_handover_conflict():
    owner = _confirmed_token("valid-code")
    applicant1 = _confirmed_token("valid-code-2")
    applicant2 = _confirmed_token("valid-code-3")
    post_id = _create_published_post(owner, FOUND_BODY)

    claim1 = _submit_claim(applicant1, post_id, "我的校园卡A").json()["data"]["id"]
    claim2 = _submit_claim(applicant2, post_id, "我的校园卡B").json()["data"]["id"]

    assert client.post(f"/api/v1/claims/{claim1}/accept", headers=_headers(owner)).status_code == 200

    resp = client.post(f"/api/v1/claims/{claim2}/accept", headers=_headers(owner))
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "HANDOVER_CONFLICT"


def test_non_owner_cannot_accept():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner, FOUND_BODY)
    claim_id = _submit_claim(applicant, post_id, "我的校园卡").json()["data"]["id"]

    resp = client.post(f"/api/v1/claims/{claim_id}/accept", headers=_headers(applicant))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"
