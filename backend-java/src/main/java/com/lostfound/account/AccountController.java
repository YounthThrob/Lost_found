package com.lostfound.account;

import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    public record CampusProfileUpdate(String school, String campus) {
    }

    public record CampusProfileView(String school, String campus, String verificationStatus,
                                    String verifiedEmail, String emailVerifiedAt) {
    }

    public record EmailVerificationSend(String email) {
    }

    public record EmailVerificationConfirm(String email, String code) {
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

    @GetMapping("/me/campus-profile")
    public Envelope.Response<CampusProfileView> getCampusProfile(Authentication authentication) {
        UserAccount user = accountService.currentUser(authentication.getName());
        return Envelope.ok(profileView(user));
    }

    @PutMapping("/me/campus-profile")
    public Envelope.Response<CampusProfileView> putCampusProfile(Authentication authentication,
                                                                 @RequestBody CampusProfileUpdate request) {
        UserAccount user = accountService.updateCampusProfile(
            authentication.getName(), request.school(), request.campus());
        return Envelope.ok(profileView(user));
    }

    @PostMapping("/me/campus-verification/email")
    public Envelope.Response<Map<String, Object>> sendCampusEmailVerification(
            Authentication authentication, @RequestBody EmailVerificationSend request) {
        int expiresInSeconds = accountService.sendCampusEmailVerification(
            authentication.getName(), request.email());
        return Envelope.ok(Map.of("expiresInSeconds", expiresInSeconds));
    }

    @PostMapping("/me/campus-verification/email/confirm")
    public Envelope.Response<CampusProfileView> confirmCampusEmailVerification(
            Authentication authentication, @RequestBody EmailVerificationConfirm request) {
        UserAccount user = accountService.confirmCampusEmailVerification(
            authentication.getName(), request.email(), request.code());
        return Envelope.ok(profileView(user));
    }

    private CampusProfileView profileView(UserAccount user) {
        return new CampusProfileView(user.school(), user.campus(), user.verificationStatus(),
            user.verifiedEmail(), user.emailVerifiedAt());
    }
}
