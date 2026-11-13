package com.lostfound.account;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailProviderTest {

    @Test
    void send_returns_expiry_when_no_whitelist() {
        EmailProvider provider = new EmailProvider("");
        assertEquals(300, provider.send("student@school.edu.cn"));
    }

    @Test
    void send_returns_expiry_when_domain_allowed() {
        EmailProvider provider = new EmailProvider("school.edu.cn,example.edu.cn");
        assertEquals(300, provider.send("student@school.edu.cn"));
        assertEquals(300, provider.send("student@example.edu.cn"));
    }

    @Test
    void send_rejects_disallowed_domain() {
        EmailProvider provider = new EmailProvider("school.edu.cn");
        BusinessException e = assertThrows(BusinessException.class,
            () -> provider.send("x@other.com"));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
    }

    @Test
    void confirm_accepts_fixed_code_only() {
        EmailProvider provider = new EmailProvider("");
        assertTrue(provider.confirm("a@b.c", "123456"));
        assertFalse(provider.confirm("a@b.c", "000000"));
        assertFalse(provider.confirm("a@b.c", ""));
    }
}
