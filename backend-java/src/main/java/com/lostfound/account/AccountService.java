package com.lostfound.account;

import com.lostfound.security.JwtService;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    public record LoginResult(String token, boolean agreementAccepted) {
    }

    private static final int MAX_FIELD_LENGTH = 100;

    private final WechatClient wechatClient;
    private final InMemoryUserStore userStore;
    private final JwtService jwtService;

    public AccountService(WechatClient wechatClient, InMemoryUserStore userStore, JwtService jwtService) {
        this.wechatClient = wechatClient;
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    public LoginResult login(String code) {
        String openid = wechatClient.code2sessionOpenid(code);
        UserAccount user = userStore.findByOpenid(openid);
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
}
