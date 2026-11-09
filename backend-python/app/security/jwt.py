import time

import jwt as pyjwt

from app import config


def issue_token(subject: str) -> str:
    now = int(time.time())
    payload = {"sub": subject, "iat": now, "exp": now + config.JWT_TTL_SECONDS}
    return pyjwt.encode(payload, config.JWT_SECRET, algorithm="HS256")


def parse_subject(token: str) -> str:
    payload = pyjwt.decode(token, config.JWT_SECRET, algorithms=["HS256"])
    return payload["sub"]
