import os


def _bool(value, default=False):
    if value is None or value == "":
        return default
    return value.lower() in ("1", "true", "yes", "on")


# 微信登录配置。生产环境通过环境变量覆盖，密钥不进版本库。
WECHAT_MOCK = _bool(os.environ.get("WECHAT_MOCK"), True)
WECHAT_APPID = os.environ.get("WECHAT_APPID", "")
WECHAT_SECRET = os.environ.get("WECHAT_SECRET", "")

# JWT 会话配置。HS256 要求密钥长度 >= 32 字节。
JWT_SECRET = os.environ.get("JWT_SECRET", "dev-only-secret-key-please-change-0123456789")
JWT_TTL_SECONDS = int(os.environ.get("JWT_TTL_SECONDS", "7200"))
