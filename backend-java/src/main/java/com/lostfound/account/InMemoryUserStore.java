package com.lostfound.account;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存用户存储。
 * 说明：walking skeleton 阶段使用内存存储以保持可运行；后续 change（资料/发布）再引入 JPA 持久化。
 */
@Component
public class InMemoryUserStore {

    private final Map<String, UserAccount> byOpenid = new ConcurrentHashMap<>();

    public UserAccount findByOpenid(String openid) {
        return findByOpenid(openid, "USER");
    }

    public UserAccount findByOpenid(String openid, String role) {
        return byOpenid.computeIfAbsent(openid,
            o -> new UserAccount(UUID.randomUUID().toString(), o, "ACTIVE", null, null, null, role,
                "SELF_DECLARED", null, null, null, null));
    }

    public UserAccount findById(String id) {
        return byOpenid.values().stream()
            .filter(u -> u.id().equals(id))
            .findFirst()
            .orElse(null);
    }

    public UserAccount confirmAgreement(String userId) {
        UserAccount user = findById(userId);
        if (user == null) {
            return null;
        }
        if (user.agreementAccepted()) {
            return user;
        }
        UserAccount updated = new UserAccount(user.id(), user.openid(), user.status(),
            Instant.now().toString(), user.schoolId(), user.campusId(), user.role(),
            user.verificationStatus(), user.verifiedEmail(), user.emailVerifiedAt(),
            user.nickname(), user.avatarKey());
        byOpenid.put(user.openid(), updated);
        return updated;
    }

    public UserAccount updateCampus(String userId, String schoolId, String campusId) {
        UserAccount user = findById(userId);
        if (user == null) {
            return null;
        }
        UserAccount updated = new UserAccount(user.id(), user.openid(), user.status(),
            user.agreementAcceptedAt(), schoolId, campusId, user.role(),
            user.verificationStatus(), user.verifiedEmail(), user.emailVerifiedAt(),
            user.nickname(), user.avatarKey());
        byOpenid.put(user.openid(), updated);
        return updated;
    }

    public UserAccount verifyEmail(String userId, String email) {
        UserAccount user = findById(userId);
        if (user == null) {
            return null;
        }
        UserAccount updated = new UserAccount(user.id(), user.openid(), user.status(),
            user.agreementAcceptedAt(), user.schoolId(), user.campusId(), user.role(),
            "EMAIL_VERIFIED", email, Instant.now().toString(),
            user.nickname(), user.avatarKey());
        byOpenid.put(user.openid(), updated);
        return updated;
    }

    public UserAccount updateProfile(String userId, String nickname, String avatarKey) {
        UserAccount user = findById(userId);
        if (user == null) {
            return null;
        }
        UserAccount updated = new UserAccount(user.id(), user.openid(), user.status(),
            user.agreementAcceptedAt(), user.schoolId(), user.campusId(), user.role(),
            user.verificationStatus(), user.verifiedEmail(), user.emailVerifiedAt(),
            nickname, avatarKey);
        byOpenid.put(user.openid(), updated);
        return updated;
    }

    public UserAccount restrict(String userId) {
        return setStatus(userId, "RESTRICTED");
    }

    public UserAccount unrestrict(String userId) {
        return setStatus(userId, "ACTIVE");
    }

    private UserAccount setStatus(String userId, String status) {
        UserAccount user = findById(userId);
        if (user == null) {
            return null;
        }
        UserAccount updated = new UserAccount(user.id(), user.openid(), status,
            user.agreementAcceptedAt(), user.schoolId(), user.campusId(), user.role(),
            user.verificationStatus(), user.verifiedEmail(), user.emailVerifiedAt(),
            user.nickname(), user.avatarKey());
        byOpenid.put(user.openid(), updated);
        return updated;
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byOpenid.clear();
    }
}
