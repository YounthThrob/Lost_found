import pytest
from fastapi.testclient import TestClient

from app.account import service as account_service
from app.account.store import InMemoryUserStore
from app.listing import service as listing_service
from app.listing.store import InMemoryItemPostStore
from app.main import app
from app.moderation import service as moderation_service
from app.moderation.store import InMemoryReportStore
from app.operations import service as operations_service
from app.operations.store import InMemoryAuditStore

client = TestClient(app)

LOST_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失校园卡",
    "school": "南方大学",
}


@pytest.fixture(autouse=True)
def fresh_stores():
    account_service._store = InMemoryUserStore()
    listing_service._store = InMemoryItemPostStore()
    moderation_service._store = InMemoryReportStore()
    operations_service._store = InMemoryAuditStore()
    yield


def _login_token(code: str) -> str:
    return client.post("/api/v1/session/wechat", json={"code": code}).json()["data"]["token"]


def _confirmed_token(code: str) -> str:
    token = _login_token(code)
    client.post("/api/v1/me/agreement", headers=_headers(token))
    return token


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def _create_published_post(token: str) -> str:
    post_id = client.post("/api/v1/posts", headers=_headers(token), json=LOST_BODY).json()["data"]["id"]
    client.post(f"/api/v1/posts/{post_id}/publish", headers=_headers(token))
    return post_id


def _user_id(token: str) -> str:
    return client.get("/api/v1/me", headers=_headers(token)).json()["data"]["id"]


def test_non_operator_rejected():
    user = _confirmed_token("valid-code")

    resp = client.post(
        "/api/v1/admin/users/some-user/restrict",
        headers=_headers(user),
        json={"reason": "违规"},
    )
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "OPERATOR_REQUIRED"

    resp = client.get("/api/v1/admin/audit-events", headers=_headers(user))
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "OPERATOR_REQUIRED"


def test_operator_removes_published_post_and_hides_from_search():
    operator = _login_token("operator-code")
    owner = _confirmed_token("valid-code")
    post_id = _create_published_post(owner)

    resp = client.post(
        f"/api/v1/admin/posts/{post_id}/remove",
        headers=_headers(operator),
        json={"reason": "违规信息"},
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "REMOVED"

    resp = client.get("/api/v1/posts", headers=_headers(owner))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 0


def test_operator_remove_nonexistent_returns_404():
    operator = _login_token("operator-code")

    resp = client.post(
        "/api/v1/admin/posts/nonexistent/remove",
        headers=_headers(operator),
        json={"reason": "x"},
    )
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_operator_remove_nonpublic_returns_409():
    operator = _login_token("operator-code")
    owner = _confirmed_token("valid-code")
    post_id = _create_published_post(owner)
    client.post(f"/api/v1/posts/{post_id}/close", headers=_headers(owner))

    resp = client.post(
        f"/api/v1/admin/posts/{post_id}/remove",
        headers=_headers(operator),
        json={"reason": "x"},
    )
    assert resp.status_code == 409
    assert resp.json()["error"]["code"] == "INVALID_STATE_TRANSITION"


def test_operator_restrict_then_blocked_from_publish_and_unrestrict():
    operator = _login_token("operator-code")
    target = _confirmed_token("valid-code-2")
    target_id = _user_id(target)

    resp = client.post(
        f"/api/v1/admin/users/{target_id}/restrict",
        headers=_headers(operator),
        json={"reason": "违规"},
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "RESTRICTED"

    resp = client.post("/api/v1/posts", headers=_headers(target), json=LOST_BODY)
    assert resp.status_code == 403
    assert resp.json()["error"]["code"] == "ACCOUNT_RESTRICTED"

    resp = client.post(
        f"/api/v1/admin/users/{target_id}/unrestrict",
        headers=_headers(operator),
        json={"reason": "解除"},
    )
    assert resp.status_code == 200
    assert resp.json()["data"]["status"] == "ACTIVE"

    resp = client.post("/api/v1/posts", headers=_headers(target), json=LOST_BODY)
    assert resp.status_code == 200


def test_operator_restrict_nonexistent_returns_404():
    operator = _login_token("operator-code")

    resp = client.post(
        "/api/v1/admin/users/nonexistent/restrict",
        headers=_headers(operator),
        json={"reason": "x"},
    )
    assert resp.status_code == 404
    assert resp.json()["error"]["code"] == "NOT_FOUND"


def test_operator_list_all_reports():
    operator = _login_token("operator-code")
    reporter = _confirmed_token("valid-code-2")
    client.post(
        "/api/v1/reports",
        headers=_headers(reporter),
        json={"targetType": "POST", "targetId": "post-1", "reason": "疑似诈骗"},
    )

    resp = client.get("/api/v1/admin/reports", headers=_headers(operator))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["reporterId"]


def test_operator_actions_write_audit_events():
    operator = _login_token("operator-code")
    owner = _confirmed_token("valid-code")
    post_id = _create_published_post(owner)

    client.post(
        f"/api/v1/admin/posts/{post_id}/remove",
        headers=_headers(operator),
        json={"reason": "违规"},
    )

    resp = client.get("/api/v1/admin/audit-events", headers=_headers(operator))
    assert resp.status_code == 200
    assert resp.json()["data"]["pagination"]["total"] == 1
    assert resp.json()["data"]["items"][0]["eventType"] == "POST_REMOVED"
    assert resp.json()["data"]["items"][0]["metadata"]["reason"] == "违规"


def test_operator_action_empty_reason_returns_422():
    operator = _login_token("operator-code")

    resp = client.post(
        "/api/v1/admin/users/some-user/restrict",
        headers=_headers(operator),
        json={"reason": ""},
    )
    assert resp.status_code == 422
    assert resp.json()["error"]["code"] == "VALIDATION_FAILED"
