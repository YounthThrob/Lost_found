package com.lostfound.account;

import com.lostfound.security.JwtService;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final WechatClient wechatClient;
    private final InMemoryUserStore userStore;
    private final JwtService jwtService;

    public AccountService(WechatClient wechatClient, InMemoryUserStore userStore, JwtService jwtService) {
        this.wechatClient = wechatClient;
        this.userStore = userStore;
        this.jwtService = jwtService;
    }

    public String login(String code) {
        String openid = wechatClient.code2sessionOpenid(code);
        UserAccount user = userStore.findByOpenid(openid);
        return jwtService.issue(user.id());
    }

    public UserAccount currentUser(String userId) {
        return userStore.findById(userId);
    }
}
