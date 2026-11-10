import pytest
from fastapi import Depends, FastAPI
from fastapi.testclient import TestClient

from app.account import service
from app.account.deps import require_agreement
from app.account.store import InMemoryUserStore
from app.main import app
from app.security.jwt import parse_subject
from app.shared.errors import AGREEMENT_NOT_ACCEPTED, BusinessError, register_exception_handlers

client = TestClient(app)


@pytest.fixture(autouse=True)
def fresh_store():
    service._store = InMemoryUserStore()
    yield


def _login_token() -> str:
    resp = client.post("/api/v1/session/wechat", json={"code": "valid-code"})
    return resp.json()["data"]["token"]


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def test_login_returns_agreement_accepted_false():
    resp = client.post("/api/v1/session/wechat", json={"code": "valid-code"})
    assert resp.status_code == 200
    assert resp.json()["data"]["agreementAccepted"] is False


def test_confirm_agreement_then_me_returns_true():
    token = _login_token()

    resp = client.post("/api/v1/me/agreement", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["agreementAccepted"] is True

    resp = client.get("/api/v1/me", headers=_headers(token))
    assert resp.status_code == 200
    assert resp.json()["data"]["agreementAccepted"] is True


def test_confirm_agreement_is_idempotent():
    token = _login_token()
    assert client.post("/api/v1/me/agreement", headers=_headers(token)).status_code == 200
    assert client.post("/api/v1/me/agreement", headers=_headers(token)).status_code == 200


def test_require_agreement_rejects_unconfirmed():
    token = _login_token()
    user_id = parse_subject(token)
    with pytest.raises(BusinessError) as exc:
        service.require_agreement(user_id)
    assert exc.value.error_code == AGREEMENT_NOT_ACCEPTED


def test_require_agreement_dependency_guards_endpoint():
    token = _login_token()
    guard_app = FastAPI()
    register_exception_handlers(guard_app)

    @guard_app.get("/test/guarded")
    def guarded(user_id: str = Depends(require_agreement)):
        return {"ok": True}

    guard_client = TestClient(guard_app)
    headers = _headers(token)

    resp = guard_client.get("/test/guarded", headers=headers)
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "AGREEMENT_NOT_ACCEPTED"

    client.post("/api/v1/me/agreement", headers=headers)

    resp = guard_client.get("/test/guarded", headers=headers)
    assert resp.status_code == 200
    assert resp.json() == {"ok": True}
