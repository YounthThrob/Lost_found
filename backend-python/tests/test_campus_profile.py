import pytest
from fastapi.testclient import TestClient

from app.account import service
from app.account.store import InMemoryUserStore
from app.main import app

client = TestClient(app)

SCHOOL_ID = "018c0000-0000-7000-8000-000000000001"


@pytest.fixture(autouse=True)
def fresh_store():
    service._store = InMemoryUserStore()
    yield


def _login_token() -> str:
    return client.post("/api/v1/session/wechat", json={"code": "valid-code"}).json()["data"]["token"]


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def _active_campus_id(token: str, name: str) -> str | None:
    """通过目录接口按校区名取到确定学校下的校区 id（校区主键为不可预测 UUID）。"""
    resp = client.get(
        f"/api/v1/directory/schools/{SCHOOL_ID}/campuses", headers=_headers(token)
    )
    for item in resp.json()["data"]["items"]:
        if item["name"] == name:
            return item["id"]
    return None


def test_get_campus_profile_unset_returns_null_and_marker():
    token = _login_token()
    resp = client.get("/api/v1/me/campus-profile", headers=_headers(token))
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["schoolId"] is None
    assert data["campusId"] is None
    assert data["verificationStatus"] == "SELF_DECLARED"


def test_put_then_get_campus_profile_returns_values():
    token = _login_token()
    headers = _headers(token)
    campus_id = _active_campus_id(token, "东校区")

    resp = client.put("/api/v1/me/campus-profile", headers=headers,
                      json={"schoolId": SCHOOL_ID, "campusId": campus_id})
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["schoolName"] == "南方大学"
    assert data["campusName"] == "东校区"
    assert data["verificationStatus"] == "SELF_DECLARED"

    resp = client.get("/api/v1/me/campus-profile", headers=headers)
    data = resp.json()["data"]
    assert data["schoolName"] == "南方大学"
    assert data["campusName"] == "东校区"


def test_put_empty_school_returns_validation_failed():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token), json={"schoolId": ""})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_put_missing_school_returns_validation_failed():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token), json={"campusId": "any-campus-id"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_put_unknown_school_returns_validation_failed():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token),
                      json={"schoolId": "00000000-0000-0000-0000-000000000000"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_campus_profile_does_not_imply_certification():
    token = _login_token()
    resp = client.put("/api/v1/me/campus-profile", headers=_headers(token), json={"schoolId": SCHOOL_ID})
    data = resp.json()["data"]
    assert data["verificationStatus"] == "SELF_DECLARED"
    assert "verified" not in data
    assert "certified" not in data


def test_update_campus_preserves_agreement():
    token = _login_token()
    headers = _headers(token)
    client.post("/api/v1/me/agreement", headers=headers)
    client.put("/api/v1/me/campus-profile", headers=headers, json={"schoolId": SCHOOL_ID})
    resp = client.get("/api/v1/me", headers=headers)
    assert resp.json()["data"]["agreementAccepted"] is True


def test_confirm_agreement_preserves_campus():
    token = _login_token()
    headers = _headers(token)
    client.put("/api/v1/me/campus-profile", headers=headers, json={"schoolId": SCHOOL_ID})
    client.post("/api/v1/me/agreement", headers=headers)
    resp = client.get("/api/v1/me/campus-profile", headers=headers)
    assert resp.json()["data"]["schoolName"] == "南方大学"
