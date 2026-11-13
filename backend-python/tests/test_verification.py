import pytest
from fastapi.testclient import TestClient

from app.account import service
from app.account.store import InMemoryUserStore
from app.account.verification import EmailProvider
from app.main import app
from app.shared.errors import BusinessError

client = TestClient(app)


@pytest.fixture(autouse=True)
def fresh_store():
    service._store = InMemoryUserStore()
    yield


def _login_token() -> str:
    return client.post("/api/v1/session/wechat", json={"code": "valid-code"}).json()["data"]["token"]


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


# ---- provider 单测 ----

def test_email_provider_send_and_confirm():
    provider = EmailProvider("")
    assert provider.send("a@b.edu.cn") == 300
    assert provider.confirm("a@b.edu.cn", "123456") is True
    assert provider.confirm("a@b.edu.cn", "000000") is False


def test_email_provider_domain_whitelist():
    provider = EmailProvider("school.edu.cn")
    assert provider.send("x@school.edu.cn") == 300
    with pytest.raises(BusinessError):
        provider.send("x@other.com")


# ---- 端点集成测试 ----

def test_send_returns_expiry():
    token = _login_token()
    resp = client.post("/api/v1/me/campus-verification/email", headers=_headers(token),
                       json={"email": "student@school.edu.cn"})
    assert resp.status_code == 200
    assert resp.json()["data"]["expiresInSeconds"] == 300


def test_send_invalid_email_returns_validation_failed():
    token = _login_token()
    resp = client.post("/api/v1/me/campus-verification/email", headers=_headers(token),
                       json={"email": "not-an-email"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"


def test_confirm_wrong_code_returns_verification_code_invalid():
    token = _login_token()
    resp = client.post("/api/v1/me/campus-verification/email/confirm", headers=_headers(token),
                       json={"email": "student@school.edu.cn", "code": "000000"})
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VERIFICATION_CODE_INVALID"


def test_confirm_success_sets_email_verified():
    token = _login_token()
    headers = _headers(token)
    resp = client.post("/api/v1/me/campus-verification/email/confirm", headers=headers,
                       json={"email": "student@school.edu.cn", "code": "123456"})
    assert resp.status_code == 200
    data = resp.json()["data"]
    assert data["verificationStatus"] == "EMAIL_VERIFIED"
    assert data["verifiedEmail"] == "student@school.edu.cn"
    assert data["emailVerifiedAt"]

    resp2 = client.get("/api/v1/me/campus-profile", headers=headers)
    assert resp2.json()["data"]["verificationStatus"] == "EMAIL_VERIFIED"


def test_reconfirm_overwrites_verified_email():
    token = _login_token()
    headers = _headers(token)
    client.post("/api/v1/me/campus-verification/email/confirm", headers=headers,
                json={"email": "student@school.edu.cn", "code": "123456"})
    resp = client.post("/api/v1/me/campus-verification/email/confirm", headers=headers,
                       json={"email": "new@school.edu.cn", "code": "123456"})
    assert resp.status_code == 200
    assert resp.json()["data"]["verifiedEmail"] == "new@school.edu.cn"
    assert resp.json()["data"]["verificationStatus"] == "EMAIL_VERIFIED"
