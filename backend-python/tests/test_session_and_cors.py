import pytest
from fastapi.testclient import TestClient

from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.main import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def fresh_store():
    account_service._store = InMemoryUserStore()
    yield


def _login_token(code: str = "valid-code") -> str:
    return client.post("/api/v1/session/wechat", json={"code": code}).json()["data"]["token"]


def test_me_with_stale_token_returns_session_expired():
    token = _login_token()
    # 模拟后端重启：重建内存用户库，JWT 仍有效但用户已不存在。
    account_service._store = InMemoryUserStore()

    resp = client.get("/api/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 401
    assert resp.json()["error"]["code"] == "SESSION_EXPIRED"


def test_cors_preflight_allows_admin_origin():
    resp = client.options(
        "/api/v1/session/wechat",
        headers={
            "Origin": "http://localhost:5173",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "content-type",
        },
    )
    assert resp.status_code == 200
    assert resp.headers.get("access-control-allow-origin") == "http://localhost:5173"


def test_cors_preflight_rejects_unknown_origin():
    resp = client.options(
        "/api/v1/session/wechat",
        headers={
            "Origin": "http://evil.example.com",
            "Access-Control-Request-Method": "POST",
        },
    )
    assert "access-control-allow-origin" not in resp.headers
