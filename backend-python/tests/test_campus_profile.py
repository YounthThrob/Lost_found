import pytest
from fastapi.testclient import TestClient

from app.account import service
from app.account.store import InMemoryUserStore
from app.main import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def fresh_store():
    service._store = InMemoryUserStore()
    yield


def _login_token() -> str:
    return client.post("/api/v1/session/wechat", json={"code": "valid-code"}).json()["data"]["token"]


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def test_get_campus_profile_unset_returns_null_and_marker():
    token = _login_token()
    resp = client.get("/api/v1/me/campus-profile", headers=_headers(token))
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["school"] is None
    assert data["campus"] is None
    assert data["verificationStatus"] == "SELF_DECLARED"


def test_put_then_get_campus_profile_returns_values():
    token = _login_token()
    headers = _headers(token)

    resp = client.put("/api/v1/me/campus-profile", headers=headers,
                      json={"school": "南方大学", "campus": "东校区"})
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["school"] == "南方大学"
    assert data["campus"] == "东校区"
    assert data["verificationStatus"] == "SELF_DECLARED"

    resp = client.get("/api/v1/me/campus-profile", headers=headers)
    data = resp.json()["data"]
    assert data["school"] == "南方大学"
    assert data["campus"] == "东校区"


def test_put_empty_school_returns_validation_failed():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token), json={"school": ""})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_put_missing_school_returns_validation_failed():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token), json={"campus": "东校区"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_campus_profile_does_not_imply_certification():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token), json={"school": "南方大学"})
    data = resp.json()["data"]
    assert data["verificationStatus"] == "SELF_DECLARED"
    assert "verified" not in data
    assert "certified" not in data


def test_update_campus_preserves_agreement():
    token = _login_token()
    headers = _headers(token)
    client.post("/api/v1/me/agreement", headers=headers)
    client.put("/api/v1/me/campus-profile", headers=headers, json={"school": "南方大学"})
    resp = client.get("/api/v1/me", headers=headers)
    assert resp.json()["data"]["agreementAccepted"] is True


def test_confirm_agreement_preserves_campus():
    token = _login_token()
    headers = _headers(token)
    client.put("/api/v1/me/campus-profile", headers=headers, json={"school": "南方大学"})
    client.post("/api/v1/me/agreement", headers=headers)
    resp = client.get("/api/v1/me/campus-profile", headers=headers)
    assert resp.json()["data"]["school"] == "南方大学"
