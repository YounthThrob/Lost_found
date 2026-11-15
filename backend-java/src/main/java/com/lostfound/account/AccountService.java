package com.lostfound.account;

import com.lostfound.directory.Campus;
import com.lostfound.directory.DirectoryService;
import com.lostfound.directory.School;
import com.lostfound.security.JwtService;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import com.lostfound.storage.Storage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountService {

    public record LoginResult(String token, boolean agreementAccepted) {
    }

    private static final int MAX_FIELD_LENGTH = 100;
    private static final int MAX_NICKNAME_LENGTH = 30;
    private static final int MAX_AVATAR_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // 运营账户 id 固定，跨进程可复现（等价不可预测标识）。
    private static final String OPERATOR_USER_ID = "018c0000-0000-7000-8000-0000000000aa";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final WechatClient wechatClient;
    private final InMemoryUserStore userStore;
    private final JwtService jwtService;
    private final VerificationProvider verificationProvider;
    private final Storage storage;
    private final DirectoryService directoryService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${operator.username}")
    private String operatorUsername;

    @Value("${operator.password}")
    private String operatorPassword;

    public AccountService(WechatClient wechatClient, InMemoryUserStore userStore, JwtService jwtService,
                          VerificationProvider verificationProvider, Storage storage,
                          DirectoryService directoryService) {
        this.wechatClient = wechatClient;
        this.userStore = userStore;
        this.jwtService = jwtService;
        this.verificationProvider = verificationProvider;
        this.storage = storage;
        this.directoryService = directoryService;
    }

    /** 启动时种子运营账户；测试重置用户存储后亦可调用重建。 */
    @PostConstruct
    public void seedOperator() {
        userStore.seedOperator(OPERATOR_USER_ID, operatorUsername, passwordEncoder.encode(operatorPassword));
    }

    public LoginResult login(String code) {
        String openid = wechatClient.code2sessionOpenid(code);
        UserAccount user = userStore.findByOpenid(openid, "USER");
        return new LoginResult(jwtService.issue(user.id()), user.agreementAccepted());
    }

    public String operatorLogin(String username, String password) {
        UserAccount user = userStore.findByUsername(username);
        if (user == null || user.passwordHash() == null
            || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return jwtService.issue(user.id());
    }

    public UserAccount currentUser(String userId) {
        return userStore.findById(userId);
    }

    public UserAccount confirmAgreement(String userId) {
        return userStore.confirmAgreement(userId);
    }

    public UserAccount updateCampusProfile(String userId, String schoolId, String campusId) {
        School school = directoryService.findSchool(schoolId);
        if (school == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String resolvedCampusId = null;
        if (campusId != null) {
            Campus campus = directoryService.findActiveCampus(school.id(), campusId);
            if (campus == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            }
            resolvedCampusId = campus.id();
        }
        return userStore.updateCampus(userId, school.id(), resolvedCampusId);
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

    public UserAccount updateProfile(String userId, String nickname) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (normalized.length() > MAX_NICKNAME_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        UserAccount user = userStore.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return userStore.updateProfile(userId, normalized, user.avatarKey());
    }

    public String setAvatar(String userId, byte[] content, String contentType) {
        String normalizedType = contentType == null ? "" : contentType.trim();
        if (!ALLOWED_AVATAR_TYPES.contains(normalizedType)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (content == null || content.length > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        UserAccount user = userStore.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String key = UUID.randomUUID().toString();
        storage.put(key, content, normalizedType);
        userStore.updateProfile(userId, user.nickname(), key);
        if (user.avatarKey() != null && !user.avatarKey().equals(key)) {
            storage.delete(user.avatarKey());
        }
        return "/media/" + key;
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
