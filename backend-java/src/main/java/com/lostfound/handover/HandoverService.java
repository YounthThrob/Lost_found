package com.lostfound.handover;

import com.lostfound.claim.Claim;
import com.lostfound.claim.ClaimStore;
import com.lostfound.listing.ItemPost;
import com.lostfound.listing.ItemPostStore;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** 交接安排、双方确认与终止服务。 */
@Service
public class HandoverService {

    public record HandoverCreate(String claimId, String timeWindow, String publicPlace) {
    }

    private static final int MAX_FIELD = 200;

    private final HandoverStore store;
    private final ClaimStore claimStore;
    private final ItemPostStore postStore;

    public HandoverService(HandoverStore store, ClaimStore claimStore, ItemPostStore postStore) {
        this.store = store;
        this.claimStore = claimStore;
        this.postStore = postStore;
    }

    public Handover create(String userId, HandoverCreate input) {
        Claim claim = requireClaim(input.claimId());
        ItemPost post = requirePost(claim.postId());
        requireParticipant(userId, claim, post);
        if (!"ACCEPTED".equals(claim.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        if (store.findByClaimId(claim.id()) != null) {
            throw new BusinessException(ErrorCode.HANDOVER_ALREADY_EXISTS);
        }
        String timeWindow = requireField(input.timeWindow());
        String publicPlace = requireField(input.publicPlace());
        Handover handover = new Handover(UUID.randomUUID().toString(), claim.id(), userId,
            timeWindow, publicPlace, null, null, "PENDING", Instant.now().toString());
        store.save(handover);
        return handover;
    }

    public Handover confirm(String userId, String handoverId) {
        Handover handover = requireHandover(handoverId);
        Claim claim = requireClaim(handover.claimId());
        ItemPost post = requirePost(claim.postId());
        requireParticipant(userId, claim, post);
        if (!"PENDING".equals(handover.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String now = Instant.now().toString();
        String ownerConfirmedAt = handover.ownerConfirmedAt();
        String claimantConfirmedAt = handover.claimantConfirmedAt();
        if (userId.equals(post.authorId()) && ownerConfirmedAt == null) {
            ownerConfirmedAt = now;
        }
        if (userId.equals(claim.applicantId()) && claimantConfirmedAt == null) {
            claimantConfirmedAt = now;
        }
        boolean completed = ownerConfirmedAt != null && claimantConfirmedAt != null;
        String status = completed ? "COMPLETED" : "PENDING";
        Handover updated = new Handover(handover.id(), handover.claimId(), handover.proposerId(),
            handover.timeWindow(), handover.publicPlace(), ownerConfirmedAt, claimantConfirmedAt,
            status, handover.createdAt());
        store.save(updated);
        if (completed) {
            claimStore.save(withStatus(claim, "COMPLETED"));
            if ("PENDING_HANDOVER".equals(post.status())) {
                postStore.save(withStatus(post, "COMPLETED"));
            }
        }
        return updated;
    }

    public Handover cancel(String userId, String handoverId) {
        Handover handover = requireHandover(handoverId);
        Claim claim = requireClaim(handover.claimId());
        ItemPost post = requirePost(claim.postId());
        requireParticipant(userId, claim, post);
        if (!"PENDING".equals(handover.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        Handover updated = new Handover(handover.id(), handover.claimId(), handover.proposerId(),
            handover.timeWindow(), handover.publicPlace(), handover.ownerConfirmedAt(),
            handover.claimantConfirmedAt(), "CANCELLED", handover.createdAt());
        store.save(updated);
        claimStore.save(withStatus(claim, "CANCELLED"));
        if ("PENDING_HANDOVER".equals(post.status())) {
            postStore.save(withStatus(post, "PUBLISHED"));
        }
        return updated;
    }

    private Handover requireHandover(String handoverId) {
        Handover handover = store.findById(handoverId);
        if (handover == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return handover;
    }

    private Claim requireClaim(String claimId) {
        Claim claim = claimStore.findById(claimId);
        if (claim == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return claim;
    }

    private ItemPost requirePost(String postId) {
        ItemPost post = postStore.findById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return post;
    }

    private void requireParticipant(String userId, Claim claim, ItemPost post) {
        if (!userId.equals(post.authorId()) && !userId.equals(claim.applicantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private String requireField(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > MAX_FIELD) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return value.trim();
    }

    private Claim withStatus(Claim claim, String status) {
        return new Claim(claim.id(), claim.postId(), claim.applicantId(), claim.statement(),
            status, claim.version(), claim.createdAt(), Instant.now().toString());
    }

    private ItemPost withStatus(ItemPost post, String status) {
        return new ItemPost(post.id(), post.authorId(), post.type(), post.category(), post.title(),
            post.publicDescription(), post.schoolId(), post.schoolName(), post.campusId(), post.campusName(),
            post.area(), status, post.createdAt(), Instant.now().toString());
    }
}
