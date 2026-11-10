package com.lostfound.account;

import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    public record LoginRequest(String code) {
    }

    @PostMapping("/session/wechat")
    public Envelope.Response<Map<String, Object>> login(@RequestBody LoginRequest request) {
        AccountService.LoginResult result = accountService.login(request.code());
        return Envelope.ok(Map.of(
            "token", result.token(),
            "agreementAccepted", result.agreementAccepted()));
    }

    @GetMapping("/me")
    public Envelope.Response<Map<String, Object>> me(Authentication authentication) {
        UserAccount user = accountService.currentUser(authentication.getName());
        return Envelope.ok(Map.of(
            "id", user.id(),
            "status", user.status(),
            "agreementAccepted", user.agreementAccepted()));
    }

    @PostMapping("/me/agreement")
    public Envelope.Response<Map<String, Object>> confirmAgreement(Authentication authentication) {
        accountService.confirmAgreement(authentication.getName());
        return Envelope.ok(Map.of("agreementAccepted", true));
    }
}
