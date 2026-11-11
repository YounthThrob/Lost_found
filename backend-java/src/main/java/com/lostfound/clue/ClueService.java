package com.lostfound.clue;

import com.lostfound.abuse.RateLimiter;
import com.lostfound.listing.ItemPost;
import com.lostfound.listing.ItemPostStore;
import com.lostfound.moderation.ContentModerator;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClueService {

    public record ClueCreate(String content) {
    }

    public record ClueDecide(String decision) {
    }

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record ClueListResult(List<Clue> items, Pagination pagination) {
    }

    private static final int MAX_CONTENT = 2000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClueStore store;
    private final ItemPostStore postStore;
    private final RateLimiter rateLimiter;
    private final ContentModerator moderator;

    public ClueService(ClueStore store, ItemPostStore postStore, RateLimiter rateLimiter,
                       ContentModerator moderator) {
        this.store = store;
        this.postStore = postStore;
        this.rateLimiter = rateLimiter;
        this.moderator = moderator;
    }

    public Clue submit(String providerId, String postId, ClueCreate input) {
        rateLimiter.check(providerId, "clue");
        ItemPost post = postStore.findById(postId);
        if (post == null || !isPublic(post.status())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"LOST".equals(post.type())) {
            throw new BusinessException(ErrorCode.CLUE_NOT_ALLOWED);
        }
        String content = requireContent(input.content());
        String status = moderator.isRejected(content, null) ? "REJECTED" : "SUBMITTED";
        Clue clue = new Clue(UUID.randomUUID().toString(), postId, providerId, content,
            status, Instant.now().toString());
        store.save(clue);
        return clue;
    }

    public ClueListResult listPendingByPost(String ownerId, String postId, int page, int pageSize) {
        requireOwnedPost(ownerId, postId);
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<Clue> all = store.listByPost(postId).stream()
            .filter(c -> "SUBMITTED".equals(c.status()))
            .toList();
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<Clue> items = from >= total ? List.of() : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new ClueListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    public Clue decide(String ownerId, String clueId, ClueDecide input) {
        Clue clue = requireOwnedClue(ownerId, clueId);
        if (!"SUBMITTED".equals(clue.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String decision = input.decision() == null ? null : input.decision().trim();
        if (!"VALID".equals(decision) && !"INVALID".equals(decision)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return withStatus(clue, "VALID".equals(decision) ? "ACCEPTED" : "CLOSED");
    }

    private Clue requireOwnedClue(String ownerId, String clueId) {
        Clue clue = store.findById(clueId);
        if (clue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ItemPost post = postStore.findById(clue.postId());
        if (post == null || !post.authorId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return clue;
    }

    private void requireOwnedPost(String ownerId, String postId) {
        ItemPost post = postStore.findById(postId);
        if (post == null || !post.authorId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private String requireContent(String content) {
        if (content == null || content.trim().isEmpty() || content.trim().length() > MAX_CONTENT) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return content.trim();
    }

    private boolean isPublic(String status) {
        return "PUBLISHED".equals(status) || "VERIFYING".equals(status);
    }

    private Clue withStatus(Clue clue, String status) {
        Clue updated = new Clue(clue.id(), clue.postId(), clue.providerId(), clue.content(),
            status, clue.createdAt());
        store.save(updated);
        return updated;
    }
}
