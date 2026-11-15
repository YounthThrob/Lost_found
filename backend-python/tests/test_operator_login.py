import pytest
from fastapi.testclient import TestClient

from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.main import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    account_service.seed_operator()
    yield


def test_operator_login_success_returns_token():
    resp = client.post("/api/v1/session/operator", json={"username": "admin", "password": "admin123"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["data"]["token"]
    assert "agreementAccepted" not in body["data"]


def test_operator_login_success_does_not_leak_password_or_hash():
    resp = client.post("/api/v1/session/operator", json={"username": "admin", "password": "admin123"})
    assert resp.status_code == 200
    text = resp.text
    assert "admin123" not in text
    assert "passwordHash" not in text
    assert "$2b$" not in text


def test_operator_login_wrong_password_returns_invalid_credentials():
    resp = client.post("/api/v1/session/operator", json={"username": "admin", "password": "wrong"})
    assert resp.status_code == 401
    assert resp.json()["error"]["code"] == "INVALID_CREDENTIALS"


def test_operator_login_wrong_username_returns_invalid_credentials():
    resp = client.post("/api/v1/session/operator", json={"username": "no-such", "password": "admin123"})
    assert resp.status_code == 401
    assert resp.json()["error"]["code"] == "INVALID_CREDENTIALS"


def test_operator_token_can_access_operations():
    token = client.post("/api/v1/session/operator", json={"username": "admin", "password": "admin123"}).json()["data"]["token"]
    resp = client.get("/api/v1/admin/audit-events", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
