package com.lostfound.listing;

import com.lostfound.account.RequiresAgreement;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    public record ItemPostView(String id, String type, String category, String title,
                               String publicDescription, String school, String campus, String area,
                               String status, String authorId, String createdAt, String updatedAt) {

        static ItemPostView from(ItemPost p) {
            return new ItemPostView(p.id(), p.type(), p.category(), p.title(), p.publicDescription(),
                p.school(), p.campus(), p.area(), p.status(), p.authorId(), p.createdAt(), p.updatedAt());
        }
    }

    public record ItemPostListData(List<ItemPostView> items, ListingService.Pagination pagination) {
    }

    @PostMapping("/posts")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> createPost(Authentication authentication,
                                                      @RequestBody ListingService.ItemPostCreate request) {
        return Envelope.ok(ItemPostView.from(
            listingService.createDraft(authentication.getName(), request)));
    }

    @PatchMapping("/posts/{postId}")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> updatePost(Authentication authentication,
                                                      @PathVariable String postId,
                                                      @RequestBody ListingService.ItemPostUpdate request) {
        return Envelope.ok(ItemPostView.from(
            listingService.updateDraft(authentication.getName(), postId, request)));
    }

    @PostMapping("/posts/{postId}/publish")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> publishPost(Authentication authentication,
                                                       @PathVariable String postId) {
        return Envelope.ok(ItemPostView.from(
            listingService.publish(authentication.getName(), postId)));
    }

    @PostMapping("/posts/{postId}/close")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> closePost(Authentication authentication,
                                                     @PathVariable String postId) {
        return Envelope.ok(ItemPostView.from(
            listingService.close(authentication.getName(), postId)));
    }

    @GetMapping("/posts/{postId}")
    public Envelope.Response<ItemPostView> getPost(Authentication authentication,
                                                   @PathVariable String postId) {
        return Envelope.ok(ItemPostView.from(
            listingService.getDetail(authentication.getName(), postId)));
    }

    @GetMapping("/posts")
    public Envelope.Response<ItemPostListData> searchPosts(Authentication authentication,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String type,
                                                           @RequestParam(required = false) String category,
                                                           @RequestParam(required = false) String school,
                                                           @RequestParam(required = false) String campus,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        ListingService.SearchResult result =
            listingService.search(keyword, type, category, school, campus, page, pageSize);
        List<ItemPostView> views = result.items().stream().map(ItemPostView::from).toList();
        ListingService.Pagination p = result.pagination();
        return Envelope.ok(new ItemPostListData(views, p));
    }
}
