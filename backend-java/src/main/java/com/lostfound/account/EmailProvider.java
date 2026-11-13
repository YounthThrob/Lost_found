package com.lostfound.account;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校园邮箱核验提供方（mock）。
 * 固定验证码 {@code 123456}、有效期 300 秒；通过 CAMPUS_EMAIL_DOMAINS 配置校园邮箱域名白名单（为空表示不限制）。
 */
@Component
public class EmailProvider implements VerificationProvider {

    private static final String FIXED_CODE = "123456";
    private static final int EXPIRES_SECONDS = 300;

    private final Set<String> allowedDomains;

    public EmailProvider(@Value("${campus-email-domains:}") String campusEmailDomains) {
        this.allowedDomains = parse(campusEmailDomains);
    }

    @Override
    public int send(String email) {
        if (!isDomainAllowed(email)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return EXPIRES_SECONDS;
    }

    @Override
    public boolean confirm(String email, String code) {
        return FIXED_CODE.equals(code);
    }

    /** 邮箱域名白名单：配置为空表示不限制；非空时精确匹配 @ 后域名。 */
    public boolean isDomainAllowed(String email) {
        if (allowedDomains.isEmpty()) {
            return true;
        }
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        return allowedDomains.contains(email.substring(at + 1));
    }

    private static Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
}
