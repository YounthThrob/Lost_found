package com.lostfound.bookmark;

import com.lostfound.listing.ItemPost;
import com.lostfound.listing.ItemPostStore;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BookmarkService {

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record BookmarkEntry(String postId, String createdAt, ItemPost post) {
    }

    public record BookmarkListResult(List<BookmarkEntry> items, Pagination pagination) {
    }

    private static final int MAX_PAGE_SIZE = 100;

    private final BookmarkStore store;
    private final ItemPostStore postStore;

    public BookmarkService(BookmarkStore store, ItemPostStore postStore) {
        this.store = store;
        this.postStore = postStore;
    }

    public BookmarkEntry bookmark(String userId, String postId) {
        ItemPost post = postStore.findById(postId);
        if (post == null || !isPublic(post.status())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Bookmark bookmark = store.find(userId, postId);
        if (bookmark == null) {
            bookmark = new Bookmark(userId, postId, Instant.now().toString());
            store.save(bookmark);
        }
        return new BookmarkEntry(bookmark.postId(), bookmark.createdAt(), post);
    }

    public void unbookmark(String userId, String postId) {
        store.remove(userId, postId);
    }

    public BookmarkListResult list(String userId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<BookmarkEntry> all = store.listByUser(userId).stream()
            .map(b -> {
                ItemPost post = postStore.findById(b.postId());
                return post == null ? null : new BookmarkEntry(b.postId(), b.createdAt(), post);
            })
            .filter(entry -> entry != null)
            .toList();
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<BookmarkEntry> items = from >= total ? List.of()
            : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new BookmarkListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    private boolean isPublic(String status) {
        return "PUBLISHED".equals(status) || "VERIFYING".equals(status);
    }
}
