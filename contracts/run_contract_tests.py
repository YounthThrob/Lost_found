#!/usr/bin/env python3
"""共享黑盒契约测试。

对 Java 与 Python 两个后端运行同一组登录/会话/协议确认用例，断言二者行为一致，
且符合 openapi/product-api.yaml 与 contracts/error-codes.yaml 的约定。

用法（在仓库根目录，用 backend-python/.venv 的 Python 运行）：
    backend-python/.venv/Scripts/python.exe contracts/run_contract_tests.py
"""
import os
import re
import subprocess
import sys
import time

import httpx

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA_PORT = 18080
PY_PORT = 18081
JWT_SECRET = "contract-test-secret-key-0123456789abcd"

JAVA_BASE = f"http://127.0.0.1:{JAVA_PORT}"
PY_BASE = f"http://127.0.0.1:{PY_PORT}"

EXPECTED = {
    "login_success": {
        "status": 200,
        "has_token": True,
        "agreement_accepted": False,
        "leaks_openid": False,
        "leaks_session_key": False,
        "keys": ["data", "requestId", "timestamp"],
        "timestamp_is_rfc3339": True,
    },
    "login_invalid": {
        "status": 401,
        "error_code": "WECHAT_CODE_INVALID",
        "error_keys": ["code", "details", "message"],
        "details_empty": True,
        "has_request_id": True,
    },
    "me_no_auth": {"status": 401, "error_code": "SESSION_EXPIRED"},
    "me_valid": {"status": 200, "has_id": True, "user_status": "ACTIVE", "agreement_accepted": False},
    "confirm_agreement": {"status": 200, "agreement_accepted": True},
    "me_after_agreement": {"status": 200, "agreement_accepted": True},
    "campus_unset": {"status": 200, "school": None, "verification_status": "SELF_DECLARED"},
    "campus_put": {"status": 200, "school": "南方大学", "campus": "东校区", "verification_status": "SELF_DECLARED"},
    "campus_get": {"status": 200, "school": "南方大学", "campus": "东校区"},
    "campus_invalid": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "listing_create_unconfirmed": {"status": 403, "error_code": "AGREEMENT_NOT_ACCEPTED"},
    "listing_create": {"status": 200, "post_status": "DRAFT", "has_id": True},
    "listing_search_draft_hidden": {"status": 200, "total": 0},
    "listing_detail_draft_hidden": {"status": 404, "error_code": "NOT_FOUND"},
    "listing_publish": {"status": 200, "post_status": "PUBLISHED"},
    "listing_detail": {"status": 200, "post_status": "PUBLISHED"},
    "listing_search_published": {
        "status": 200,
        "total": 1,
        "pagination": {"page": 1, "pageSize": 20, "total": 1, "hasMore": False},
    },
    "pagination_page_zero": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "pagination_page_size_overflow": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "pagination_non_integer_page": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "pagination_page_size_max": {"status": 200, "page_size": 100},
    "pagination_beyond_last_page": {"status": 200, "total": 1, "items": [], "has_more": False},
    "listing_publish_again": {"status": 409, "error_code": "INVALID_STATE_TRANSITION"},
    "listing_close": {"status": 200, "post_status": "CLOSED"},
    "listing_search_closed_hidden": {"status": 200, "total": 0},
    "listing_invalid_close": {"status": 409, "error_code": "INVALID_STATE_TRANSITION"},
    "listing_not_found": {"status": 404, "error_code": "NOT_FOUND"},
    "listing_empty_title": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "moderation_reject": {"status": 200, "post_status": "REJECTED"},
    "moderation_reject_hidden": {"status": 200, "total": 0},
    "report_create": {"status": 200, "report_status": "SUBMITTED", "has_id": True},
    "report_invalid_type": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "report_me_list": {"status": 200, "total": 1},
    "claim_submit": {"status": 200, "claim_status": "SUBMITTED"},
    "claim_duplicate": {"status": 409, "error_code": "CLAIM_DUPLICATE"},
    "claim_not_claimable": {"status": 409, "error_code": "POST_NOT_CLAIMABLE"},
    "claim_list_owner": {"status": 200, "total": 1},
    "claim_list_nonowner": {"status": 404, "error_code": "NOT_FOUND"},
    "claim_request_info": {"status": 200, "claim_status": "NEEDS_MORE_INFO"},
    "claim_accept": {"status": 200, "claim_status": "ACCEPTED"},
    "claim_post_after_accept": {"status": 200, "post_status": "PENDING_HANDOVER"},
    "claim_reject": {"status": 200, "claim_status": "REJECTED"},
    "claim_post_after_reject": {"status": 200, "post_status": "PUBLISHED"},
    "claim_handover_conflict": {"status": 409, "error_code": "HANDOVER_CONFLICT"},
    "message_send": {"status": 200, "message_type": "TEXT"},
    "message_list": {"status": 200, "total": 1},
    "block_create": {"status": 200, "has_blocked_user_id": True},
    "block_duplicate": {"status": 200},
    "block_self": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "block_nonexistent": {"status": 404, "error_code": "NOT_FOUND"},
    "block_list": {"status": 200, "total": 1},
    "message_blocked": {"status": 403, "error_code": "BLOCKED"},
    "block_unblock": {"status": 200},
    "block_list_after_unblock": {"status": 200, "total": 0},
    "message_after_unblock": {"status": 200, "message_type": "SUGGEST_TIME"},
    "message_nonparticipant": {"status": 404, "error_code": "NOT_FOUND"},
    "message_not_accepted": {"status": 409, "error_code": "INVALID_STATE_TRANSITION"},
    "message_invalid_type": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "message_empty_content": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "handover_create": {"status": 200, "handover_status": "PENDING", "proposer_is_owner": True},
    "handover_duplicate": {"status": 409, "error_code": "HANDOVER_ALREADY_EXISTS"},
    "handover_nonparticipant": {"status": 404, "error_code": "NOT_FOUND"},
    "handover_not_accepted": {"status": 409, "error_code": "INVALID_STATE_TRANSITION"},
    "handover_empty_field": {"status": 422, "error_code": "VALIDATION_FAILED"},
    "handover_owner_confirm": {"status": 200, "handover_status": "PENDING", "owner_confirmed": True, "claimant_confirmed": False},
    "handover_both_confirm": {"status": 200, "handover_status": "COMPLETED"},
    "handover_claim_completed": {"status": 200, "claim_status": "COMPLETED"},
    "handover_post_completed": {"status": 200, "post_status": "COMPLETED"},
    "handover_cancel": {"status": 200, "handover_status": "CANCELLED"},
    "handover_cancel_claim": {"status": 200, "claim_status": "CANCELLED"},
    "handover_cancel_post_published": {"status": 200, "post_status": "PUBLISHED"},
    "handover_confirm_cancelled": {"status": 409, "error_code": "INVALID_STATE_TRANSITION"},
    "claim_rate_limited": {"status": 429, "error_code": "CLAIM_RATE_LIMITED", "requests_before_429": 20},
    "post_rate_limited": {"status": 429, "error_code": "RATE_LIMITED", "requests_before_429": 20},
}


POST_BODY = {
    "type": "LOST",
    "category": "校园卡",
    "title": "遗失校园卡",
    "publicDescription": "在图书馆附近遗失一张校园卡",
    "school": "南方大学",
    "campus": "东校区",
}

BANNED_BODY = {
    "type": "LOST",
    "category": "其他",
    "title": "高价回收",
    "publicDescription": "要求先转账到指定账户",
    "school": "南方大学",
}

FOUND_BODY = {
    "type": "FOUND",
    "category": "校园卡",
    "title": "拾到校园卡",
    "publicDescription": "在食堂拾到一张校园卡",
    "school": "南方大学",
}


def probe(base: str) -> dict:
    sig = {}
    r = httpx.post(base + "/api/v1/session/wechat", json={"code": "valid-code"})
    sig["login_success"] = {
        "status": r.status_code,
        "has_token": bool(r.json().get("data", {}).get("token")),
        "agreement_accepted": r.json().get("data", {}).get("agreementAccepted"),
        "leaks_openid": "openid" in r.text,
        "leaks_session_key": "session_key" in r.text,
        "keys": sorted(r.json().keys()),
        "timestamp_is_rfc3339": bool(re.fullmatch(
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z", r.json().get("timestamp", ""))),
    }
    token = r.json()["data"]["token"]
    headers = {"Authorization": "Bearer " + token}

    r2 = httpx.post(base + "/api/v1/session/wechat", json={"code": "bad-code"})
    sig["login_invalid"] = {
        "status": r2.status_code,
        "error_code": r2.json().get("error", {}).get("code"),
        "error_keys": sorted(r2.json().get("error", {}).keys()),
        "details_empty": r2.json().get("error", {}).get("details") == {},
        "has_request_id": bool(r2.json().get("requestId")),
    }

    r3 = httpx.get(base + "/api/v1/me")
    sig["me_no_auth"] = {
        "status": r3.status_code,
        "error_code": r3.json().get("error", {}).get("code"),
    }

    r4 = httpx.get(base + "/api/v1/me", headers=headers)
    sig["me_valid"] = {
        "status": r4.status_code,
        "has_id": bool(r4.json().get("data", {}).get("id")),
        "user_status": r4.json().get("data", {}).get("status"),
        "agreement_accepted": r4.json().get("data", {}).get("agreementAccepted"),
    }

    r5 = httpx.post(base + "/api/v1/me/agreement", headers=headers)
    sig["confirm_agreement"] = {
        "status": r5.status_code,
        "agreement_accepted": r5.json().get("data", {}).get("agreementAccepted"),
    }

    r6 = httpx.get(base + "/api/v1/me", headers=headers)
    sig["me_after_agreement"] = {
        "status": r6.status_code,
        "agreement_accepted": r6.json().get("data", {}).get("agreementAccepted"),
    }

    r7 = httpx.get(base + "/api/v1/me/campus-profile", headers=headers)
    sig["campus_unset"] = {
        "status": r7.status_code,
        "school": r7.json().get("data", {}).get("school"),
        "verification_status": r7.json().get("data", {}).get("verificationStatus"),
    }

    r8 = httpx.put(base + "/api/v1/me/campus-profile", headers=headers,
                   json={"school": "南方大学", "campus": "东校区"})
    sig["campus_put"] = {
        "status": r8.status_code,
        "school": r8.json().get("data", {}).get("school"),
        "campus": r8.json().get("data", {}).get("campus"),
        "verification_status": r8.json().get("data", {}).get("verificationStatus"),
    }

    r9 = httpx.get(base + "/api/v1/me/campus-profile", headers=headers)
    sig["campus_get"] = {
        "status": r9.status_code,
        "school": r9.json().get("data", {}).get("school"),
        "campus": r9.json().get("data", {}).get("campus"),
    }

    r10 = httpx.put(base + "/api/v1/me/campus-profile", headers=headers, json={"school": ""})
    sig["campus_invalid"] = {
        "status": r10.status_code,
        "error_code": r10.json().get("error", {}).get("code"),
    }

    # ---- listing ----
    token2 = httpx.post(base + "/api/v1/session/wechat", json={"code": "valid-code-2"}).json()["data"]["token"]
    headers2 = {"Authorization": "Bearer " + token2}

    r11 = httpx.post(base + "/api/v1/posts", headers=headers2, json=POST_BODY)
    sig["listing_create_unconfirmed"] = {
        "status": r11.status_code,
        "error_code": r11.json().get("error", {}).get("code"),
    }

    r12 = httpx.post(base + "/api/v1/posts", headers=headers, json=POST_BODY)
    sig["listing_create"] = {
        "status": r12.status_code,
        "post_status": r12.json().get("data", {}).get("status"),
        "has_id": bool(r12.json().get("data", {}).get("id")),
    }
    post_id = r12.json()["data"]["id"]

    r13 = httpx.get(base + "/api/v1/posts", headers=headers)
    sig["listing_search_draft_hidden"] = {
        "status": r13.status_code,
        "total": r13.json().get("data", {}).get("pagination", {}).get("total"),
    }

    r14 = httpx.get(base + f"/api/v1/posts/{post_id}", headers=headers2)
    sig["listing_detail_draft_hidden"] = {
        "status": r14.status_code,
        "error_code": r14.json().get("error", {}).get("code"),
    }

    r15 = httpx.post(base + f"/api/v1/posts/{post_id}/publish", headers=headers)
    sig["listing_publish"] = {
        "status": r15.status_code,
        "post_status": r15.json().get("data", {}).get("status"),
    }

    r16 = httpx.get(base + f"/api/v1/posts/{post_id}", headers=headers2)
    sig["listing_detail"] = {
        "status": r16.status_code,
        "post_status": r16.json().get("data", {}).get("status"),
    }

    r17 = httpx.get(base + "/api/v1/posts", headers=headers)
    sig["listing_search_published"] = {
        "status": r17.status_code,
        "total": r17.json().get("data", {}).get("pagination", {}).get("total"),
        "pagination": r17.json().get("data", {}).get("pagination"),
    }

    # ---- 分页契约 ----
    rp0 = httpx.get(base + "/api/v1/posts", headers=headers, params={"page": 0})
    sig["pagination_page_zero"] = {
        "status": rp0.status_code,
        "error_code": rp0.json().get("error", {}).get("code"),
    }

    rp1 = httpx.get(base + "/api/v1/posts", headers=headers, params={"pageSize": 101})
    sig["pagination_page_size_overflow"] = {
        "status": rp1.status_code,
        "error_code": rp1.json().get("error", {}).get("code"),
    }

    rp2 = httpx.get(base + "/api/v1/posts", headers=headers, params={"page": "abc"})
    sig["pagination_non_integer_page"] = {
        "status": rp2.status_code,
        "error_code": rp2.json().get("error", {}).get("code"),
    }

    rp3 = httpx.get(base + "/api/v1/posts", headers=headers, params={"pageSize": 100})
    sig["pagination_page_size_max"] = {
        "status": rp3.status_code,
        "page_size": rp3.json().get("data", {}).get("pagination", {}).get("pageSize"),
    }

    rp4 = httpx.get(base + "/api/v1/posts", headers=headers, params={"page": 999})
    sig["pagination_beyond_last_page"] = {
        "status": rp4.status_code,
        "total": rp4.json().get("data", {}).get("pagination", {}).get("total"),
        "items": rp4.json().get("data", {}).get("items"),
        "has_more": rp4.json().get("data", {}).get("pagination", {}).get("hasMore"),
    }

    r18 = httpx.post(base + f"/api/v1/posts/{post_id}/publish", headers=headers)
    sig["listing_publish_again"] = {
        "status": r18.status_code,
        "error_code": r18.json().get("error", {}).get("code"),
    }

    r19 = httpx.post(base + f"/api/v1/posts/{post_id}/close", headers=headers)
    sig["listing_close"] = {
        "status": r19.status_code,
        "post_status": r19.json().get("data", {}).get("status"),
    }

    r20 = httpx.get(base + "/api/v1/posts", headers=headers)
    sig["listing_search_closed_hidden"] = {
        "status": r20.status_code,
        "total": r20.json().get("data", {}).get("pagination", {}).get("total"),
    }

    draft_id = httpx.post(base + "/api/v1/posts", headers=headers, json=POST_BODY).json()["data"]["id"]
    r21 = httpx.post(base + f"/api/v1/posts/{draft_id}/close", headers=headers)
    sig["listing_invalid_close"] = {
        "status": r21.status_code,
        "error_code": r21.json().get("error", {}).get("code"),
    }

    r22 = httpx.get(base + "/api/v1/posts/nonexistent-id", headers=headers)
    sig["listing_not_found"] = {
        "status": r22.status_code,
        "error_code": r22.json().get("error", {}).get("code"),
    }

    r23 = httpx.post(base + "/api/v1/posts", headers=headers, json={**POST_BODY, "title": ""})
    sig["listing_empty_title"] = {
        "status": r23.status_code,
        "error_code": r23.json().get("error", {}).get("code"),
    }

    # ---- moderation / report ----
    banned_id = httpx.post(base + "/api/v1/posts", headers=headers, json=BANNED_BODY).json()["data"]["id"]
    r24 = httpx.post(base + f"/api/v1/posts/{banned_id}/publish", headers=headers)
    sig["moderation_reject"] = {
        "status": r24.status_code,
        "post_status": r24.json().get("data", {}).get("status"),
    }

    r25 = httpx.get(base + "/api/v1/posts", headers=headers)
    sig["moderation_reject_hidden"] = {
        "status": r25.status_code,
        "total": r25.json().get("data", {}).get("pagination", {}).get("total"),
    }

    r26 = httpx.post(base + "/api/v1/reports", headers=headers,
                     json={"targetType": "POST", "targetId": "post-abc123", "reason": "疑似诈骗"})
    sig["report_create"] = {
        "status": r26.status_code,
        "report_status": r26.json().get("data", {}).get("status"),
        "has_id": bool(r26.json().get("data", {}).get("id")),
    }

    r27 = httpx.post(base + "/api/v1/reports", headers=headers,
                     json={"targetType": "OTHER", "targetId": "post-abc123", "reason": "其他"})
    sig["report_invalid_type"] = {
        "status": r27.status_code,
        "error_code": r27.json().get("error", {}).get("code"),
    }

    r28 = httpx.get(base + "/api/v1/reports/me", headers=headers)
    sig["report_me_list"] = {
        "status": r28.status_code,
        "total": r28.json().get("data", {}).get("pagination", {}).get("total"),
    }

    # ---- claim-verification ----
    httpx.post(base + "/api/v1/me/agreement", headers=headers2)
    token3 = httpx.post(base + "/api/v1/session/wechat", json={"code": "valid-code-3"}).json()["data"]["token"]
    headers3 = {"Authorization": "Bearer " + token3}
    httpx.post(base + "/api/v1/me/agreement", headers=headers3)

    found_id = httpx.post(base + "/api/v1/posts", headers=headers, json=FOUND_BODY).json()["data"]["id"]
    httpx.post(base + f"/api/v1/posts/{found_id}/publish", headers=headers)

    r29 = httpx.post(base + f"/api/v1/posts/{found_id}/claims", headers=headers2,
                     json={"statement": "我的校园卡，学号尾号 0427"})
    sig["claim_submit"] = {
        "status": r29.status_code,
        "claim_status": r29.json().get("data", {}).get("status"),
    }
    claim_id = r29.json()["data"]["id"]

    r30 = httpx.post(base + f"/api/v1/posts/{found_id}/claims", headers=headers2,
                     json={"statement": "重复申请"})
    sig["claim_duplicate"] = {
        "status": r30.status_code,
        "error_code": r30.json().get("error", {}).get("code"),
    }

    lost_id = httpx.post(base + "/api/v1/posts", headers=headers, json=POST_BODY).json()["data"]["id"]
    httpx.post(base + f"/api/v1/posts/{lost_id}/publish", headers=headers)
    r31 = httpx.post(base + f"/api/v1/posts/{lost_id}/claims", headers=headers2,
                     json={"statement": "不该能认领寻物"})
    sig["claim_not_claimable"] = {
        "status": r31.status_code,
        "error_code": r31.json().get("error", {}).get("code"),
    }

    r32 = httpx.get(base + f"/api/v1/posts/{found_id}/claims", headers=headers)
    sig["claim_list_owner"] = {
        "status": r32.status_code,
        "total": r32.json().get("data", {}).get("pagination", {}).get("total"),
    }

    r33 = httpx.get(base + f"/api/v1/posts/{found_id}/claims", headers=headers2)
    sig["claim_list_nonowner"] = {
        "status": r33.status_code,
        "error_code": r33.json().get("error", {}).get("code"),
    }

    r34 = httpx.post(base + f"/api/v1/claims/{claim_id}/request-info", headers=headers)
    sig["claim_request_info"] = {
        "status": r34.status_code,
        "claim_status": r34.json().get("data", {}).get("status"),
    }

    r35 = httpx.post(base + f"/api/v1/claims/{claim_id}/accept", headers=headers)
    sig["claim_accept"] = {
        "status": r35.status_code,
        "claim_status": r35.json().get("data", {}).get("status"),
    }

    r36 = httpx.get(base + f"/api/v1/posts/{found_id}", headers=headers)
    sig["claim_post_after_accept"] = {
        "status": r36.status_code,
        "post_status": r36.json().get("data", {}).get("status"),
    }

    found2_id = httpx.post(base + "/api/v1/posts", headers=headers, json=FOUND_BODY).json()["data"]["id"]
    httpx.post(base + f"/api/v1/posts/{found2_id}/publish", headers=headers)
    claim2_id = httpx.post(base + f"/api/v1/posts/{found2_id}/claims", headers=headers3,
                           json={"statement": "另一个申请"}).json()["data"]["id"]
    r37 = httpx.post(base + f"/api/v1/claims/{claim2_id}/reject", headers=headers)
    sig["claim_reject"] = {
        "status": r37.status_code,
        "claim_status": r37.json().get("data", {}).get("status"),
    }

    r38 = httpx.get(base + f"/api/v1/posts/{found2_id}", headers=headers)
    sig["claim_post_after_reject"] = {
        "status": r38.status_code,
        "post_status": r38.json().get("data", {}).get("status"),
    }

    found3_id = httpx.post(base + "/api/v1/posts", headers=headers, json=FOUND_BODY).json()["data"]["id"]
    httpx.post(base + f"/api/v1/posts/{found3_id}/publish", headers=headers)
    claim_a = httpx.post(base + f"/api/v1/posts/{found3_id}/claims", headers=headers2,
                         json={"statement": "申请人A"}).json()["data"]["id"]
    claim_b = httpx.post(base + f"/api/v1/posts/{found3_id}/claims", headers=headers3,
                         json={"statement": "申请人B"}).json()["data"]["id"]
    httpx.post(base + f"/api/v1/claims/{claim_a}/accept", headers=headers)
    r39 = httpx.post(base + f"/api/v1/claims/{claim_b}/accept", headers=headers)
    sig["claim_handover_conflict"] = {
        "status": r39.status_code,
        "error_code": r39.json().get("error", {}).get("code"),
    }

    # ---- safe-communication ----
    # 会话 = 已接受的认领申请 claim_id；参与者为发布者(user1) 与 申请人(user2)
    applicant2_id = httpx.get(base + f"/api/v1/posts/{found_id}/claims", headers=headers) \
        .json()["data"]["items"][0]["applicantId"]

    r40 = httpx.post(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers2,
                     json={"type": "TEXT", "content": "明天下午3点方便吗"})
    sig["message_send"] = {
        "status": r40.status_code,
        "message_type": r40.json().get("data", {}).get("type"),
    }

    r41 = httpx.get(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers)
    sig["message_list"] = {
        "status": r41.status_code,
        "total": r41.json().get("data", {}).get("pagination", {}).get("total"),
    }

    r42 = httpx.post(base + "/api/v1/blocks", headers=headers, json={"blockedUserId": applicant2_id})
    sig["block_create"] = {
        "status": r42.status_code,
        "has_blocked_user_id": bool(r42.json().get("data", {}).get("blockedUserId")),
    }

    r43 = httpx.post(base + "/api/v1/blocks", headers=headers, json={"blockedUserId": applicant2_id})
    sig["block_duplicate"] = {"status": r43.status_code}

    me_id = httpx.get(base + "/api/v1/me", headers=headers).json()["data"]["id"]
    r44 = httpx.post(base + "/api/v1/blocks", headers=headers, json={"blockedUserId": me_id})
    sig["block_self"] = {
        "status": r44.status_code,
        "error_code": r44.json().get("error", {}).get("code"),
    }

    r45 = httpx.post(base + "/api/v1/blocks", headers=headers, json={"blockedUserId": "nonexistent-user"})
    sig["block_nonexistent"] = {
        "status": r45.status_code,
        "error_code": r45.json().get("error", {}).get("code"),
    }

    r46 = httpx.get(base + "/api/v1/me/blocks", headers=headers)
    sig["block_list"] = {
        "status": r46.status_code,
        "total": r46.json().get("data", {}).get("pagination", {}).get("total"),
    }

    r47 = httpx.post(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers2,
                     json={"type": "TEXT", "content": "还在吗"})
    sig["message_blocked"] = {
        "status": r47.status_code,
        "error_code": r47.json().get("error", {}).get("code"),
    }

    r48 = httpx.delete(base + f"/api/v1/blocks/{applicant2_id}", headers=headers)
    sig["block_unblock"] = {"status": r48.status_code}

    r49 = httpx.get(base + "/api/v1/me/blocks", headers=headers)
    sig["block_list_after_unblock"] = {
        "status": r49.status_code,
        "total": r49.json().get("data", {}).get("pagination", {}).get("total"),
    }

    r50 = httpx.post(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers2,
                     json={"type": "SUGGEST_TIME", "content": "明天下午3点"})
    sig["message_after_unblock"] = {
        "status": r50.status_code,
        "message_type": r50.json().get("data", {}).get("type"),
    }

    r51 = httpx.get(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers3)
    sig["message_nonparticipant"] = {
        "status": r51.status_code,
        "error_code": r51.json().get("error", {}).get("code"),
    }

    r52 = httpx.post(base + f"/api/v1/conversations/{claim_b}/messages", headers=headers3,
                     json={"type": "TEXT", "content": "hi"})
    sig["message_not_accepted"] = {
        "status": r52.status_code,
        "error_code": r52.json().get("error", {}).get("code"),
    }

    r53 = httpx.post(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers2,
                     json={"type": "INVALID_TYPE", "content": "x"})
    sig["message_invalid_type"] = {
        "status": r53.status_code,
        "error_code": r53.json().get("error", {}).get("code"),
    }

    r54 = httpx.post(base + f"/api/v1/conversations/{claim_id}/messages", headers=headers2,
                     json={"type": "TEXT", "content": ""})
    sig["message_empty_content"] = {
        "status": r54.status_code,
        "error_code": r54.json().get("error", {}).get("code"),
    }

    # ---- item-handover ----
    def new_accepted_claim(applicant_headers):
        pid = httpx.post(base + "/api/v1/posts", headers=headers, json=FOUND_BODY).json()["data"]["id"]
        httpx.post(base + f"/api/v1/posts/{pid}/publish", headers=headers)
        cid = httpx.post(base + f"/api/v1/posts/{pid}/claims", headers=applicant_headers,
                         json={"statement": "我的校园卡"}).json()["data"]["id"]
        httpx.post(base + f"/api/v1/claims/{cid}/accept", headers=headers)
        return pid, cid

    hp_id, hc_id = new_accepted_claim(headers2)
    r55 = httpx.post(base + "/api/v1/handovers", headers=headers,
                     json={"claimId": hc_id, "timeWindow": "明天下午3点", "publicPlace": "图书馆一楼"})
    sig["handover_create"] = {
        "status": r55.status_code,
        "handover_status": r55.json().get("data", {}).get("status"),
        "proposer_is_owner": r55.json().get("data", {}).get("proposerId") == me_id,
    }
    handover_id = r55.json()["data"]["id"]

    r56 = httpx.post(base + "/api/v1/handovers", headers=headers2,
                     json={"claimId": hc_id, "timeWindow": "后天", "publicPlace": "校门口"})
    sig["handover_duplicate"] = {
        "status": r56.status_code,
        "error_code": r56.json().get("error", {}).get("code"),
    }

    r57 = httpx.post(base + "/api/v1/handovers", headers=headers3,
                     json={"claimId": hc_id, "timeWindow": "明天", "publicPlace": "校门口"})
    sig["handover_nonparticipant"] = {
        "status": r57.status_code,
        "error_code": r57.json().get("error", {}).get("code"),
    }

    r58 = httpx.post(base + "/api/v1/handovers", headers=headers3,
                     json={"claimId": claim_b, "timeWindow": "明天", "publicPlace": "校门口"})
    sig["handover_not_accepted"] = {
        "status": r58.status_code,
        "error_code": r58.json().get("error", {}).get("code"),
    }

    _, hc2_id = new_accepted_claim(headers2)
    r59 = httpx.post(base + "/api/v1/handovers", headers=headers,
                     json={"claimId": hc2_id, "timeWindow": "", "publicPlace": "校门口"})
    sig["handover_empty_field"] = {
        "status": r59.status_code,
        "error_code": r59.json().get("error", {}).get("code"),
    }

    r60 = httpx.post(base + f"/api/v1/handovers/{handover_id}/confirm", headers=headers)
    sig["handover_owner_confirm"] = {
        "status": r60.status_code,
        "handover_status": r60.json().get("data", {}).get("status"),
        "owner_confirmed": r60.json().get("data", {}).get("ownerConfirmedAt") is not None,
        "claimant_confirmed": r60.json().get("data", {}).get("claimantConfirmedAt") is not None,
    }

    r61 = httpx.post(base + f"/api/v1/handovers/{handover_id}/confirm", headers=headers2)
    sig["handover_both_confirm"] = {
        "status": r61.status_code,
        "handover_status": r61.json().get("data", {}).get("status"),
    }

    r62 = httpx.get(base + f"/api/v1/posts/{hp_id}/claims", headers=headers)
    sig["handover_claim_completed"] = {
        "status": r62.status_code,
        "claim_status": r62.json().get("data", {}).get("items", [{}])[0].get("status"),
    }

    r63 = httpx.get(base + f"/api/v1/posts/{hp_id}", headers=headers)
    sig["handover_post_completed"] = {
        "status": r63.status_code,
        "post_status": r63.json().get("data", {}).get("status"),
    }

    hp2_id, hc3_id = new_accepted_claim(headers2)
    h2_id = httpx.post(base + "/api/v1/handovers", headers=headers,
                       json={"claimId": hc3_id, "timeWindow": "明天", "publicPlace": "校门口"}) \
        .json()["data"]["id"]
    r64 = httpx.post(base + f"/api/v1/handovers/{h2_id}/cancel", headers=headers2)
    sig["handover_cancel"] = {
        "status": r64.status_code,
        "handover_status": r64.json().get("data", {}).get("status"),
    }

    r65 = httpx.get(base + f"/api/v1/posts/{hp2_id}/claims", headers=headers)
    sig["handover_cancel_claim"] = {
        "status": r65.status_code,
        "claim_status": r65.json().get("data", {}).get("items", [{}])[0].get("status"),
    }

    r66 = httpx.get(base + f"/api/v1/posts/{hp2_id}", headers=headers)
    sig["handover_cancel_post_published"] = {
        "status": r66.status_code,
        "post_status": r66.json().get("data", {}).get("status"),
    }

    r67 = httpx.post(base + f"/api/v1/handovers/{h2_id}/confirm", headers=headers)
    sig["handover_confirm_cancelled"] = {
        "status": r67.status_code,
        "error_code": r67.json().get("error", {}).get("code"),
    }

    # ---- abuse-prevention (rate limiting) ----
    # 使用独立用户 valid-code-4，连续请求直到触发 429，记录触发前通过的请求数（应等于 limits 阈值）。
    token4 = httpx.post(base + "/api/v1/session/wechat", json={"code": "valid-code-4"}).json()["data"]["token"]
    headers4 = {"Authorization": "Bearer " + token4}
    httpx.post(base + "/api/v1/me/agreement", headers=headers4)

    claim_before = 0
    claim_resp = None
    for _ in range(25):
        claim_resp = httpx.post(base + "/api/v1/posts/dummy-post/claims", headers=headers4,
                                json={"statement": "x"})
        if claim_resp.status_code == 429:
            break
        claim_before += 1
    sig["claim_rate_limited"] = {
        "status": claim_resp.status_code,
        "error_code": claim_resp.json().get("error", {}).get("code"),
        "requests_before_429": claim_before,
    }

    post_before = 0
    post_resp = None
    for _ in range(25):
        post_resp = httpx.post(base + "/api/v1/posts", headers=headers4, json=POST_BODY)
        if post_resp.status_code == 429:
            break
        post_before += 1
    sig["post_rate_limited"] = {
        "status": post_resp.status_code,
        "error_code": post_resp.json().get("error", {}).get("code"),
        "requests_before_429": post_before,
    }
    return sig


def wait_ready(base: str, timeout: float = 120.0) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            r = httpx.get(base + "/api/v1/me", timeout=2.0)
            if r.status_code == 401:
                return
        except httpx.HTTPError:
            pass
        time.sleep(1.0)
    raise RuntimeError(f"server at {base} did not become ready within {timeout}s")


def terminate(proc: subprocess.Popen) -> None:
    """可靠地终止后端进程（Windows 用 taskkill /F /T 强制结束进程树，避免遗留孤儿进程占用端口）。"""
    if proc.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(
            ["taskkill", "/F", "/T", "/PID", str(proc.pid)],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    else:
        proc.terminate()
    try:
        proc.wait(timeout=10)
    except subprocess.TimeoutExpired:
        proc.kill()


def main() -> int:
    java_jar = os.path.join(REPO, "backend-java", "target", "backend-java-0.1.0.jar")
    java_env = {**os.environ, "WECHAT_MOCK": "true", "JWT_SECRET": JWT_SECRET, "SERVER_PORT": str(JAVA_PORT)}
    py_env = {**os.environ, "WECHAT_MOCK": "true", "JWT_SECRET": JWT_SECRET}

    java_proc = subprocess.Popen(
        ["java", "-jar", java_jar],
        cwd=os.path.join(REPO, "backend-java"),
        env=java_env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    py_proc = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "app.main:app", "--port", str(PY_PORT)],
        cwd=os.path.join(REPO, "backend-python"),
        env=py_env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )

    try:
        wait_ready(JAVA_BASE)
        wait_ready(PY_BASE)
        java_sig = probe(JAVA_BASE)
        py_sig = probe(PY_BASE)

        failures = []
        for name, expected in EXPECTED.items():
            for backend, sig in (("java", java_sig), ("python", py_sig)):
                if sig[name] != expected:
                    failures.append(f"{backend}.{name}: got {sig[name]!r}, expected {expected!r}")

        if java_sig != py_sig:
            failures.append(f"parity mismatch:\n  java={java_sig!r}\n  python={py_sig!r}")

        if failures:
            print("CONTRACT TESTS FAILED")
            for f in failures:
                print("  - " + f)
            return 1

        print("CONTRACT TESTS PASSED (java == python == expected)")
        for name in EXPECTED:
            print(f"  {name}: {java_sig[name]!r}")
        return 0
    finally:
        terminate(java_proc)
        terminate(py_proc)


if __name__ == "__main__":
    sys.exit(main())
