package com.lostfound.listing;

import com.lostfound.abuse.RateLimiter;
import com.lostfound.moderation.ContentModerator;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import com.lostfound.storage.Storage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
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

    public record Stats(int foundCount, int lostCount, int completedToday) {
    }

    private static final int MAX_TITLE = 100;
    private static final int MAX_CATEGORY = 50;
    private static final int MAX_DESCRIPTION = 2000;
    private static final int MAX_FIELD = 100;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_IMAGES_PER_POST = 9;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ItemPostStore store;
    private final ItemImageStore imageStore;
    private final ContentModerator moderator;
    private final RateLimiter rateLimiter;
    private final Storage storage;

    public ListingService(ItemPostStore store, ItemImageStore imageStore, ContentModerator moderator,
                          RateLimiter rateLimiter, Storage storage) {
        this.store = store;
        this.imageStore = imageStore;
        this.moderator = moderator;
        this.rateLimiter = rateLimiter;
        this.storage = storage;
    }

    public ItemPost createDraft(String authorId, ItemPostCreate input) {
        rateLimiter.check(authorId, "post");
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

    /** 运营下架：仅公开状态（PUBLISHED/VERIFYING/PENDING_HANDOVER）可迁移为 REMOVED。 */
    public ItemPost remove(String postId) {
        ItemPost post = store.findById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!isRemovable(post.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        ItemPost updated = withStatus(post, "REMOVED", Instant.now().toString());
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

    public SearchResult listMyPosts(String authorId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<ItemPost> all = store.listByAuthor(authorId);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<ItemPost> items = from >= total ? List.of() : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new SearchResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    /** 广场统计：招领/寻物公开数量与今日（北京时间 UTC+8）完成交接数量。 */
    public Stats stats() {
        int foundCount = (int) store.listVisible().stream()
            .filter(p -> "FOUND".equals(p.type()))
            .count();
        int lostCount = (int) store.listVisible().stream()
            .filter(p -> "LOST".equals(p.type()))
            .count();
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        int completedToday = (int) store.listAll().stream()
            .filter(p -> "COMPLETED".equals(p.status()))
            .filter(p -> p.updatedAt() != null
                && Instant.parse(p.updatedAt()).atZone(zone).toLocalDate().equals(today))
            .count();
        return new Stats(foundCount, lostCount, completedToday);
    }

    public ItemImage uploadImage(String authorId, String postId, byte[] content, String contentType) {
        ItemPost post = requireOwned(authorId, postId);
        if (!"DRAFT".equals(post.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String normalizedType = contentType == null ? "" : contentType.trim();
        if (!ALLOWED_IMAGE_TYPES.contains(normalizedType)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (content == null || content.length > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        int sortOrder = imageStore.listByPost(postId).size();
        if (sortOrder >= MAX_IMAGES_PER_POST) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        String id = UUID.randomUUID().toString();
        String storageKey = UUID.randomUUID().toString();
        storage.put(storageKey, content, normalizedType);
        ItemImage image = new ItemImage(id, postId, storageKey, sortOrder, "PENDING");
        imageStore.save(image);
        return image;
    }

    public void deleteImage(String authorId, String postId, String imageId) {
        ItemPost post = requireOwned(authorId, postId);
        if (!"DRAFT".equals(post.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        ItemImage image = imageStore.findById(imageId);
        if (image == null || !image.postId().equals(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        storage.delete(image.storageKey());
        imageStore.delete(imageId);
    }

    public List<ItemImage> listImages(String postId) {
        return imageStore.listByPost(postId);
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

    private boolean isRemovable(String status) {
        return "PUBLISHED".equals(status) || "VERIFYING".equals(status)
            || "PENDING_HANDOVER".equals(status);
    }

    private ItemPost withStatus(ItemPost post, String status, String now) {
        return new ItemPost(post.id(), post.authorId(), post.type(), post.category(), post.title(),
            post.publicDescription(), post.school(), post.campus(), post.area(),
            status, post.createdAt(), now);
    }
}
