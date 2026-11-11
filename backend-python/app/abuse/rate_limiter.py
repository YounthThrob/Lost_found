import time
from threading import Lock

from app.shared.errors import CLAIM_RATE_LIMITED, RATE_LIMITED, BusinessError

# 必须与 contracts/rate-limits.yaml 保持一致。
WINDOW_SECONDS = 60
LIMITS = {
    "claim": 20,
    "post": 20,
    "message": 30,
    "report": 20,
}

_counters: dict[str, tuple[int, int]] = {}  # key -> (window, count)
_lock = Lock()


def check(user_id: str, operation: str) -> None:
    """对指定用户与操作做一次限流计数；达到上限即抛出对应限流错误码。"""
    limit = LIMITS.get(operation)
    if limit is None:
        return
    window = int(time.time() // WINDOW_SECONDS)
    key = f"{user_id}:{operation}"
    with _lock:
        w, count = _counters.get(key, (None, 0))
        if w != window:
            w, count = window, 0
        if count >= limit:
            raise BusinessError(CLAIM_RATE_LIMITED if operation == "claim" else RATE_LIMITED)
        _counters[key] = (window, count + 1)


def clear() -> None:
    """仅供测试重置状态。"""
    with _lock:
        _counters.clear()
