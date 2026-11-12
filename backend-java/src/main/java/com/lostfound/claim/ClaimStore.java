package com.lostfound.claim;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存认领申请存储。 */
@Component
public class ClaimStore {

    private final Map<String, Claim> byId = new ConcurrentHashMap<>();

    public void save(Claim claim) {
        byId.put(claim.id(), claim);
    }

    public Claim findById(String id) {
        return byId.get(id);
    }

    public List<Claim> listByPost(String postId) {
        return byId.values().stream()
            .filter(c -> c.postId().equals(postId))
            .sorted(Comparator.comparing(Claim::createdAt, Comparator.reverseOrder())
                .thenComparing(Claim::id))
            .toList();
    }

    public List<Claim> listByApplicant(String applicantId) {
        return byId.values().stream()
            .filter(c -> c.applicantId().equals(applicantId))
            .sorted(Comparator.comparing(Claim::createdAt, Comparator.reverseOrder())
                .thenComparing(Claim::id))
            .toList();
    }

    /** 同一申请人对同一信息进行中的申请。 */
    public Claim findActive(String applicantId, String postId) {
        return byId.values().stream()
            .filter(c -> c.applicantId().equals(applicantId)
                && c.postId().equals(postId)
                && isActive(c.status()))
            .findFirst()
            .orElse(null);
    }

    public boolean existsAcceptedOnPost(String postId) {
        return byId.values().stream()
            .anyMatch(c -> c.postId().equals(postId) && "ACCEPTED".equals(c.status()));
    }

    public boolean hasOtherActive(String postId, String excludeClaimId) {
        return byId.values().stream()
            .anyMatch(c -> c.postId().equals(postId)
                && !c.id().equals(excludeClaimId)
                && isActive(c.status()));
    }

    private boolean isActive(String status) {
        return "SUBMITTED".equals(status) || "NEEDS_MORE_INFO".equals(status) || "ACCEPTED".equals(status);
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
