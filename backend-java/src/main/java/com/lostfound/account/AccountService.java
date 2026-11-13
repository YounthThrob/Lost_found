package com.lostfound.account;

import com.lostfound.security.JwtService;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AccountService {

    public record LoginResult(String token, boolean agreementAccepted) {
    }

    private static final int MAX_FIELD_LENGTH = 100;

    private static final String OPERATOR_OPENID = "openid_operator_001";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final WechatClient wechatClient;
    private final InMemoryUserStore userStore;
    private final JwtService jwtService;
    private final VerificationProvider verificationProvider;

    public AccountService(WechatClient wechatClient, InMemoryUserStore userStore, JwtService jwtService,
                          VerificationProvider verificationProvider) {
        this.wechatClient = wechatClient;
        this.userStore = userStore;
        this.jwtService = jwtService;
        this.verificationProvider = verificationProvider;
    }

    public LoginResult login(String code) {
        String openid = wechatClient.code2sessionOpenid(code);
        String role = OPERATOR_OPENID.equals(openid) ? "OPERATOR" : "USER";
        UserAccount user = userStore.findByOpenid(openid, role);
        return new LoginResult(jwtService.issue(user.id()), user.agreementAccepted());
    }

    public UserAccount currentUser(String userId) {
        return userStore.findById(userId);
    }

    public UserAccount confirmAgreement(String userId) {
        return userStore.confirmAgreement(userId);
    }

    public UserAccount updateCampusProfile(String userId, String school, String campus) {
        String normalizedSchool = school == null ? "" : school.trim();
        String normalizedCampus = campus == null || campus.trim().isEmpty() ? null : campus.trim();
        if (normalizedSchool.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (normalizedSchool.length() > MAX_FIELD_LENGTH
                || (normalizedCampus != null && normalizedCampus.length() > MAX_FIELD_LENGTH)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return userStore.updateCampus(userId, normalizedSchool, normalizedCampus);
    }

    public int sendCampusEmailVerification(String userId, String email) {
        return verificationProvider.send(normalizeEmail(email));
    }

    public UserAccount confirmCampusEmailVerification(String userId, String email, String code) {
        String normalized = normalizeEmail(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (!verificationProvider.confirm(normalized, code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        return userStore.verifyEmail(userId, normalized);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String normalized = email.trim();
        if (normalized.length() > MAX_FIELD_LENGTH || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return normalized;
    }
}
