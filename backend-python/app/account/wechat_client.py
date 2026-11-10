import httpx

from app import config
from app.shared.errors import WECHAT_CODE_INVALID, BusinessError


def code2session_openid(code: str) -> str:
    """微信 code2session。mock 模式使用确定性桩（code=valid-code -> openid_test_001）。"""
    if not code:
        raise BusinessError(WECHAT_CODE_INVALID)
    if config.WECHAT_MOCK:
        if code == "valid-code":
            return "openid_test_001"
        if code == "valid-code-2":
            return "openid_test_002"
        raise BusinessError(WECHAT_CODE_INVALID)
    return _real_code2session(code)


def _real_code2session(code: str) -> str:
    url = "https://api.weixin.qq.com/sns/jscode2session"
    params = {
        "appid": config.WECHAT_APPID,
        "secret": config.WECHAT_SECRET,
        "js_code": code,
        "grant_type": "authorization_code",
    }
    resp = httpx.get(url, params=params)
    data = resp.json()
    if not data.get("openid"):
        raise BusinessError(WECHAT_CODE_INVALID)
    return data["openid"]
