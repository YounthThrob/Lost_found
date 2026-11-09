from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_login_success_returns_token_without_sensitive_fields():
    resp = client.post("/api/v1/session/wechat", json={"code": "valid-code"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["data"]["token"]
    assert "openid" not in resp.text
    assert "session_key" not in resp.text


def test_login_invalid_code_returns_wechat_code_invalid():
    resp = client.post("/api/v1/session/wechat", json={"code": "bad-code"})
    assert resp.status_code == 401
    assert resp.json()["error"]["code"] == "WECHAT_CODE_INVALID"


def test_me_without_session_returns_session_expired():
    resp = client.get("/api/v1/me")
    assert resp.status_code == 401
    assert resp.json()["error"]["code"] == "SESSION_EXPIRED"


def test_me_with_valid_session_returns_user():
    token = client.post("/api/v1/session/wechat", json={"code": "valid-code"}).json()["data"]["token"]
    resp = client.get("/api/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
    assert resp.json()["data"]["id"]
    assert resp.json()["data"]["status"] == "ACTIVE"
