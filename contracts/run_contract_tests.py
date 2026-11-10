#!/usr/bin/env python3
"""共享黑盒契约测试。

对 Java 与 Python 两个后端运行同一组登录/会话/协议确认用例，断言二者行为一致，
且符合 openapi/product-api.yaml 与 contracts/error-codes.yaml 的约定。

用法（在仓库根目录，用 backend-python/.venv 的 Python 运行）：
    backend-python/.venv/Scripts/python.exe contracts/run_contract_tests.py
"""
import os
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
    },
    "login_invalid": {"status": 401, "error_code": "WECHAT_CODE_INVALID"},
    "me_no_auth": {"status": 401, "error_code": "SESSION_EXPIRED"},
    "me_valid": {"status": 200, "has_id": True, "user_status": "ACTIVE", "agreement_accepted": False},
    "confirm_agreement": {"status": 200, "agreement_accepted": True},
    "me_after_agreement": {"status": 200, "agreement_accepted": True},
    "campus_unset": {"status": 200, "school": None, "verification_status": "SELF_DECLARED"},
    "campus_put": {"status": 200, "school": "南方大学", "campus": "东校区", "verification_status": "SELF_DECLARED"},
    "campus_get": {"status": 200, "school": "南方大学", "campus": "东校区"},
    "campus_invalid": {"status": 422, "error_code": "VALIDATION_FAILED"},
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
    }
    token = r.json()["data"]["token"]
    headers = {"Authorization": "Bearer " + token}

    r2 = httpx.post(base + "/api/v1/session/wechat", json={"code": "bad-code"})
    sig["login_invalid"] = {
        "status": r2.status_code,
        "error_code": r2.json().get("error", {}).get("code"),
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
        for proc in (java_proc, py_proc):
            proc.terminate()
        for proc in (java_proc, py_proc):
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()


if __name__ == "__main__":
    sys.exit(main())
