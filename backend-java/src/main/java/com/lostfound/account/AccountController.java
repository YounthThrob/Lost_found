package com.lostfound.account;

import com.lostfound.directory.DirectoryService;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;
    private final DirectoryService directoryService;

    public AccountController(AccountService accountService, DirectoryService directoryService) {
        this.accountService = accountService;
        this.directoryService = directoryService;
    }

    public record LoginRequest(String code) {
    }

    public record ProfileUpdate(String nickname) {
    }

    public record ProfileView(String nickname, String avatarUrl) {
    }

    public record CampusProfileUpdate(String schoolId, String campusId) {
    }

    public record CampusProfileView(String schoolId, String schoolName, String campusId, String campusName,
                                    String verificationStatus, String verifiedEmail, String emailVerifiedAt) {
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
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.id());
        data.put("status", user.status());
        data.put("agreementAccepted", user.agreementAccepted());
        data.put("nickname", user.nickname());
        data.put("avatarUrl", avatarUrl(user));
        return Envelope.ok(data);
    }

    @PutMapping("/me/profile")
    public Envelope.Response<ProfileView> putProfile(Authentication authentication,
                                                     @RequestBody ProfileUpdate request) {
        UserAccount user = accountService.updateProfile(authentication.getName(), request.nickname());
        return Envelope.ok(meProfileView(user));
    }

    @PostMapping("/me/avatar")
    public Envelope.Response<Map<String, Object>> uploadAvatar(Authentication authentication,
                                                               @RequestParam("file") MultipartFile file)
            throws IOException {
        String url = accountService.setAvatar(
            authentication.getName(), file.getBytes(), file.getContentType());
        return Envelope.ok(Map.of("avatarUrl", url));
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
            authentication.getName(), request.schoolId(), request.campusId());
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
        return new CampusProfileView(user.schoolId(), directoryService.findSchoolName(user.schoolId()),
            user.campusId(), directoryService.findCampusName(user.campusId()),
            user.verificationStatus(), user.verifiedEmail(), user.emailVerifiedAt());
    }

    private ProfileView meProfileView(UserAccount user) {
        return new ProfileView(user.nickname(), avatarUrl(user));
    }

    private String avatarUrl(UserAccount user) {
        return user.avatarKey() == null ? null : "/media/" + user.avatarKey();
    }
}
