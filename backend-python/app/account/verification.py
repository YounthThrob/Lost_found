from typing import Protocol

from app.shared.errors import VALIDATION_FAILED, BusinessError


class VerificationProvider(Protocol):
    """身份验证提供方抽象。MVP 为校园邮箱核验，后续可替换为微信「学生验证」等实现。"""

    def send(self, email: str) -> int:
        """发送验证码，返回验证码有效期（秒）。域名不允许时抛 VALIDATION_FAILED。"""
        ...

    def confirm(self, email: str, code: str) -> bool:
        """校验验证码是否通过。"""
        ...


def _parse_domains(raw: str) -> set[str]:
    if not raw or not raw.strip():
        return set()
    return {s.strip() for s in raw.split(",") if s.strip()}


class EmailProvider:
    """校园邮箱核验提供方（mock）。固定验证码、有效期 300 秒、CAMPUS_EMAIL_DOMAINS 域名白名单。"""

    FIXED_CODE = "123456"
    EXPIRES_SECONDS = 300

    def __init__(self, campus_email_domains: str = ""):
        self._allowed_domains = _parse_domains(campus_email_domains)

    def send(self, email: str) -> int:
        if not self.is_domain_allowed(email):
            raise BusinessError(VALIDATION_FAILED)
        return self.EXPIRES_SECONDS

    def confirm(self, email: str, code: str) -> bool:
        return code == self.FIXED_CODE

    def is_domain_allowed(self, email: str) -> bool:
        if not self._allowed_domains:
            return True
        at = email.find("@")
        if at < 0 or at == len(email) - 1:
            return False
        return email[at + 1 :] in self._allowed_domains
