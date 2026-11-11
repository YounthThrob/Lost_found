package com.lostfound.bookmark;

import com.lostfound.listing.ListingController;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BookmarkController {

    private final BookmarkService service;

    public BookmarkController(BookmarkService service) {
        this.service = service;
    }

    public record BookmarkView(String postId, String createdAt, ListingController.ItemPostView post) {
        static BookmarkView from(BookmarkService.BookmarkEntry entry) {
            return new BookmarkView(entry.postId(), entry.createdAt(),
                ListingController.ItemPostView.from(entry.post()));
        }
    }

    public record BookmarkListData(List<BookmarkView> items, BookmarkService.Pagination pagination) {
    }

    @PostMapping("/posts/{postId}/bookmark")
    public Envelope.Response<BookmarkView> bookmark(Authentication authentication,
                                                    @PathVariable String postId) {
        return Envelope.ok(BookmarkView.from(
            service.bookmark(authentication.getName(), postId)));
    }

    @DeleteMapping("/posts/{postId}/bookmark")
    public Envelope.Response<Map<String, Object>> unbookmark(Authentication authentication,
                                                             @PathVariable String postId) {
        service.unbookmark(authentication.getName(), postId);
        return Envelope.ok(Map.of());
    }

    @GetMapping("/me/bookmarks")
    public Envelope.Response<BookmarkListData> listBookmarks(Authentication authentication,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize) {
        BookmarkService.BookmarkListResult result =
            service.list(authentication.getName(), page, pageSize);
        List<BookmarkView> views = result.items().stream().map(BookmarkView::from).toList();
        return Envelope.ok(new BookmarkListData(views, result.pagination()));
    }
}
