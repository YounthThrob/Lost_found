package com.lostfound.listing;

import com.lostfound.moderation.ContentModerator;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ListingService {

    public record ItemPostCreate(String type, String category, String title,
                                 String publicDescription, String school, String campus, String area) {
    }

    public record ItemPostUpdate(String type, String category, String title,
                                 String publicDescription, String school, String campus, String area) {
    }

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record SearchResult(List<ItemPost> items, Pagination pagination) {
    }

    private static final int MAX_TITLE = 100;
    private static final int MAX_CATEGORY = 50;
    private static final int MAX_DESCRIPTION = 2000;
    private static final int MAX_FIELD = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final ItemPostStore store;
    private final ContentModerator moderator;

    public ListingService(ItemPostStore store, ContentModerator moderator) {
        this.store = store;
        this.moderator = moderator;
    }

    public ItemPost createDraft(String authorId, ItemPostCreate input) {
        String type = normalizeType(input.type());
        validate(type, input.category(), input.title(), input.publicDescription(), input.school(),
            input.campus(), input.area());
        String now = Instant.now().toString();
        ItemPost post = new ItemPost(UUID.randomUUID().toString(), authorId, type,
            input.category().trim(), input.title().trim(), input.publicDescription().trim(),
            input.school().trim(), normalize(input.campus()), normalize(input.area()),
            "DRAFT", now, now);
        store.save(post);
        return post;
    }

    public ItemPost updateDraft(String authorId, String postId, ItemPostUpdate input) {
        ItemPost post = requireOwned(authorId, postId);
        if (!"DRAFT".equals(post.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String type = normalizeType(input.type());
        validate(type, input.category(), input.title(), input.publicDescription(), input.school(),
            input.campus(), input.area());
        String now = Instant.now().toString();
        ItemPost updated = new ItemPost(post.id(), post.authorId(), type,
            input.category().trim(), input.title().trim(), input.publicDescription().trim(),
            input.school().trim(), normalize(input.campus()), normalize(input.area()),
            post.status(), post.createdAt(), now);
        store.save(updated);
        return updated;
    }

    public ItemPost publish(String authorId, String postId) {
        ItemPost post = requireOwned(authorId, postId);
        if (!"DRAFT".equals(post.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String status = moderator.isRejected(post.title(), post.publicDescription()) ? "REJECTED" : "PUBLISHED";
        ItemPost updated = withStatus(post, status, Instant.now().toString());
        store.save(updated);
        return updated;
    }

    public ItemPost close(String authorId, String postId) {
        ItemPost post = requireOwned(authorId, postId);
        if (!"PUBLISHED".equals(post.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        ItemPost updated = withStatus(post, "CLOSED", Instant.now().toString());
        store.save(updated);
        return updated;
    }

    public ItemPost getDetail(String viewerId, String postId) {
        ItemPost post = store.findById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!isPublic(post.status()) && !post.authorId().equals(viewerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return post;
    }

    public SearchResult search(String keyword, String type, String category, String school,
                               String campus, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<ItemPost> all = store.listVisible().stream()
            .filter(p -> matches(type, p.type()))
            .filter(p -> matches(category, p.category()))
            .filter(p -> matches(school, p.school()))
            .filter(p -> matches(campus, p.campus()))
            .filter(p -> keyword == null || keyword.isBlank()
                || p.title().toLowerCase().contains(keyword.trim().toLowerCase())
                || p.publicDescription().toLowerCase().contains(keyword.trim().toLowerCase()))
            .toList();
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<ItemPost> items = from >= total ? List.of() : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new SearchResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    private ItemPost requireOwned(String authorId, String postId) {
        ItemPost post = store.findById(postId);
        if (post == null || !post.authorId().equals(authorId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return post;
    }

    private void validate(String type, String category, String title, String publicDescription,
                          String school, String campus, String area) {
        if (!"LOST".equals(type) && !"FOUND".equals(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        requireText(title, MAX_TITLE);
        requireText(category, MAX_CATEGORY);
        requireText(publicDescription, MAX_DESCRIPTION);
        requireText(school, MAX_FIELD);
        if (campus != null && campus.trim().length() > MAX_FIELD) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (area != null && area.trim().length() > MAX_FIELD) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private String normalizeType(String type) {
        return type == null ? null : type.trim();
    }

    private void requireText(String value, int max) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > max) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean matches(String filter, String value) {
        return filter == null || filter.isBlank() || filter.equals(value);
    }

    private boolean isPublic(String status) {
        return "PUBLISHED".equals(status) || "VERIFYING".equals(status);
    }

    private ItemPost withStatus(ItemPost post, String status, String now) {
        return new ItemPost(post.id(), post.authorId(), post.type(), post.category(), post.title(),
            post.publicDescription(), post.school(), post.campus(), post.area(),
            status, post.createdAt(), now);
    }
}
