package com.lostfound.claim;

import com.lostfound.abuse.RateLimiter;
import com.lostfound.listing.ItemPost;
import com.lostfound.listing.ItemPostStore;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {

    public record ClaimCreate(String statement) {
    }

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record ClaimListResult(List<Claim> items, Pagination pagination) {
    }

    private static final int MAX_STATEMENT = 2000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClaimStore store;
    private final ItemPostStore postStore;
    private final RateLimiter rateLimiter;

    public ClaimService(ClaimStore store, ItemPostStore postStore, RateLimiter rateLimiter) {
        this.store = store;
        this.postStore = postStore;
        this.rateLimiter = rateLimiter;
    }

    public Claim submit(String applicantId, String postId, ClaimCreate input) {
        rateLimiter.check(applicantId, "claim");
        ItemPost post = postStore.findById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"FOUND".equals(post.type()) || !isClaimable(post.status()) || post.authorId().equals(applicantId)) {
            throw new BusinessException(ErrorCode.POST_NOT_CLAIMABLE);
        }
        if (store.findActive(applicantId, postId) != null) {
            throw new BusinessException(ErrorCode.CLAIM_DUPLICATE);
        }
        String statement = requireStatement(input.statement());
        String now = Instant.now().toString();
        Claim claim = new Claim(UUID.randomUUID().toString(), postId, applicantId, statement,
            "SUBMITTED", 1, now, now);
        store.save(claim);
        if ("PUBLISHED".equals(post.status())) {
            postStore.save(withStatus(post, "VERIFYING", now));
        }
        return claim;
    }

    public ClaimListResult listByPost(String ownerId, String postId, int page, int pageSize) {
        requireOwnedPost(ownerId, postId);
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<Claim> all = store.listByPost(postId);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<Claim> items = from >= total ? List.of() : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new ClaimListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    public ClaimListResult listMyClaims(String applicantId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<Claim> all = store.listByApplicant(applicantId);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<Claim> items = from >= total ? List.of() : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new ClaimListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    public Claim requestInfo(String ownerId, String claimId) {
        Claim claim = requireOwnedClaim(ownerId, claimId);
        if (!"SUBMITTED".equals(claim.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        return withStatus(claim, "NEEDS_MORE_INFO");
    }

    public Claim accept(String ownerId, String claimId) {
        Claim claim = requireOwnedClaim(ownerId, claimId);
        if (!"SUBMITTED".equals(claim.status()) && !"NEEDS_MORE_INFO".equals(claim.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (store.existsAcceptedOnPost(claim.postId())) {
            throw new BusinessException(ErrorCode.HANDOVER_CONFLICT);
        }
        Claim updated = withStatus(claim, "ACCEPTED");
        ItemPost post = postStore.findById(claim.postId());
        if (post != null && "VERIFYING".equals(post.status())) {
            postStore.save(withStatus(post, "PENDING_HANDOVER", Instant.now().toString()));
        }
        return updated;
    }

    public Claim reject(String ownerId, String claimId) {
        Claim claim = requireOwnedClaim(ownerId, claimId);
        if (!"SUBMITTED".equals(claim.status()) && !"NEEDS_MORE_INFO".equals(claim.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        Claim updated = withStatus(claim, "REJECTED");
        ItemPost post = postStore.findById(claim.postId());
        if (post != null && "VERIFYING".equals(post.status()) && !store.hasOtherActive(claim.postId(), claimId)) {
            postStore.save(withStatus(post, "PUBLISHED", Instant.now().toString()));
        }
        return updated;
    }

    private Claim requireOwnedClaim(String ownerId, String claimId) {
        Claim claim = store.findById(claimId);
        if (claim == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ItemPost post = postStore.findById(claim.postId());
        if (post == null || !post.authorId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return claim;
    }

    private void requireOwnedPost(String ownerId, String postId) {
        ItemPost post = postStore.findById(postId);
        if (post == null || !post.authorId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private String requireStatement(String statement) {
        if (statement == null || statement.trim().isEmpty() || statement.trim().length() > MAX_STATEMENT) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return statement.trim();
    }

    private boolean isClaimable(String status) {
        return "PUBLISHED".equals(status) || "VERIFYING".equals(status);
    }

    private Claim withStatus(Claim claim, String status) {
        Claim updated = new Claim(claim.id(), claim.postId(), claim.applicantId(), claim.statement(),
            status, claim.version(), claim.createdAt(), Instant.now().toString());
        store.save(updated);
        return updated;
    }

    private ItemPost withStatus(ItemPost post, String status, String now) {
        return new ItemPost(post.id(), post.authorId(), post.type(), post.category(), post.title(),
            post.publicDescription(), post.schoolId(), post.schoolName(), post.campusId(), post.campusName(),
            post.area(), status, post.createdAt(), now);
    }
}
