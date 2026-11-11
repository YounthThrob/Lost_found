import pytest
from fastapi.testclient import TestClient

from app.abuse import rate_limiter
from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.claim import service as claim_service
from app.claim.store import InMemoryClaimStore
from app.handover import service as handover_service
from app.handover.store import InMemoryHandoverStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app

client = TestClient(app)

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
    handover_service._store = InMemoryHandoverStore()
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


def _create_published_post(token: str) -> str:
    post_id = client.post("/api/v1/posts", headers=_headers(token), json=FOUND_BODY).json()["data"]["id"]
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    return post_id


def _accepted_claim(owner_token: str, applicant_token: str) -> tuple[str, str]:
    post_id = _create_published_post(owner_token)
    claim_id = client.post(f"/api/v1/posts/{post_id}/claims", headers=_headers(applicant_token),
                           json={"statement": "我的校园卡"}).json()["data"]["id"]
    client.post(f"/api/v1/claims/{claim_id}/accept", headers=_headers(owner_token))
    return post_id, claim_id


def _create_handover(token: str, claim_id: str) -> str:
    return client.post("/api/v1/handovers", headers=_headers(token), json={
        "claimId": claim_id, "timeWindow": "明天下午3点", "publicPlace": "图书馆一楼",
    }).json()["data"]["id"]


def test_create_returns_pending():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    owner_id = _me_id(owner)
    _, claim_id = _accepted_claim(owner, applicant)

    resp = client.post("/api/v1/handovers", headers=_headers(owner), json={
        "claimId": claim_id, "timeWindow": "明天下午3点", "publicPlace": "图书馆一楼",
    })
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["status"] == "PENDING"
    assert data["proposerId"] == owner_id
    assert data["ownerConfirmedAt"] is None
    assert data["claimantConfirmedAt"] is None


def test_duplicate_create_returns_409():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    _, claim_id = _accepted_claim(owner, applicant)

    client.post("/api/v1/handovers", headers=_headers(owner), json={
        "claimId": claim_id, "timeWindow": "明天下午3点", "publicPlace": "图书馆一楼",
    })
    resp = client.post("/api/v1/handovers", headers=_headers(applicant), json={
        "claimId": claim_id, "timeWindow": "后天", "publicPlace": "校门口",
    })
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "HANDOVER_ALREADY_EXISTS"


def test_non_participant_create_returns_404():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    third = _confirmed_token("valid-code-3")
    _, claim_id = _accepted_claim(owner, applicant)

    resp = client.post("/api/v1/handovers", headers=_headers(third), json={
        "claimId": claim_id, "timeWindow": "明天", "publicPlace": "校门口",
    })
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_create_on_unaccepted_claim_returns_409():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id = _create_published_post(owner)
    claim_id = client.post(f"/api/v1/posts/{post_id}/claims", headers=_headers(applicant),
                           json={"statement": "我的校园卡"}).json()["data"]["id"]

    resp = client.post("/api/v1/handovers", headers=_headers(owner), json={
        "claimId": claim_id, "timeWindow": "明天", "publicPlace": "校门口",
    })
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_create_empty_field_returns_422():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    _, claim_id = _accepted_claim(owner, applicant)

    resp = client.post("/api/v1/handovers", headers=_headers(owner), json={
        "claimId": claim_id, "timeWindow": "", "publicPlace": "校门口",
    })
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_both_confirm_completes_and_couples_state():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id, claim_id = _accepted_claim(owner, applicant)
    handover_id = _create_handover(owner, claim_id)

    resp = client.post(f"/api/v1/handovers/{handover_id}/confirm", headers=_headers(owner))
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["status"] == "PENDING"
    assert data["ownerConfirmedAt"] is not None
    assert data["claimantConfirmedAt"] is None

    resp = client.post(f"/api/v1/handovers/{handover_id}/confirm", headers=_headers(applicant))
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["status"] == "COMPLETED"
    assert data["ownerConfirmedAt"] is not None
    assert data["claimantConfirmedAt"] is not None

    resp = client.get(f"/api/v1/posts/{post_id}/claims", headers=_headers(owner))
    assert resp.json()["data"]["items"][0]["status"] == "COMPLETED"

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(owner))
    assert resp.json()["data"]["status"] == "COMPLETED"


def test_repeat_confirm_is_idempotent():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    _, claim_id = _accepted_claim(owner, applicant)
    handover_id = _create_handover(owner, claim_id)

    client.post(f"/api/v1/handovers/{handover_id}/confirm", headers=_headers(owner))
    resp = client.post(f"/api/v1/handovers/{handover_id}/confirm", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "PENDING"


def test_cancel_returns_cancelled_and_post_published():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    post_id, claim_id = _accepted_claim(owner, applicant)
    handover_id = _create_handover(owner, claim_id)

    resp = client.post(f"/api/v1/handovers/{handover_id}/cancel", headers=_headers(applicant))
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "CANCELLED"

    resp = client.get(f"/api/v1/posts/{post_id}/claims", headers=_headers(owner))
    assert resp.json()["data"]["items"][0]["status"] == "CANCELLED"

    resp = client.get(f"/api/v1/posts/{post_id}", headers=_headers(owner))
    assert resp.json()["data"]["status"] == "PUBLISHED"


def test_confirm_on_cancelled_returns_409():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    _, claim_id = _accepted_claim(owner, applicant)
    handover_id = _create_handover(owner, claim_id)
    client.post(f"/api/v1/handovers/{handover_id}/cancel", headers=_headers(owner))

    resp = client.post(f"/api/v1/handovers/{handover_id}/confirm", headers=_headers(owner))
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_non_participant_confirm_returns_404():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    third = _confirmed_token("valid-code-3")
    _, claim_id = _accepted_claim(owner, applicant)
    handover_id = _create_handover(owner, claim_id)

    resp = client.post(f"/api/v1/handovers/{handover_id}/confirm", headers=_headers(third))
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_create_without_agreement_returns_403():
    owner = _confirmed_token("valid-code")
    applicant = _confirmed_token("valid-code-2")
    _, claim_id = _accepted_claim(owner, applicant)
    non_confirmed = _login_token("valid-code-3")

    resp = client.post("/api/v1/handovers", headers=_headers(non_confirmed), json={
        "claimId": claim_id, "timeWindow": "明天", "publicPlace": "校门口",
    })
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "AGREEMENT_NOT_ACCEPTED"
