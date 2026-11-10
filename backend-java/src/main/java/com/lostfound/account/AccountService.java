package com.lostfound.account;

import com.lostfound.security.JwtService;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    public record LoginResult(String token, boolean agreementAccepted) {
    }

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
}
