package com.lostfound.listing;

import com.lostfound.account.RequiresAgreement;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    public record ItemImageView(String id, String url, int sortOrder) {

        public static ItemImageView of(ItemImage image) {
            return new ItemImageView(image.id(), "/media/" + image.storageKey(), image.sortOrder());
        }
    }

    public record ItemPostView(String id, String type, String category, String title,
                               String publicDescription, String school, String campus, String area,
                               String status, String authorId, String createdAt, String updatedAt,
                               List<ItemImageView> images) {

        public static ItemPostView from(ItemPost p, List<ItemImage> images) {
            List<ItemImageView> views = images.stream().map(ItemImageView::of).toList();
            return new ItemPostView(p.id(), p.type(), p.category(), p.title(), p.publicDescription(),
                p.school(), p.campus(), p.area(), p.status(), p.authorId(), p.createdAt(), p.updatedAt(),
                views);
        }
    }

    public record ItemPostListData(List<ItemPostView> items, ListingService.Pagination pagination) {
    }

    private ItemPostView view(ItemPost p) {
        return ItemPostView.from(p, listingService.listImages(p.id()));
    }

    @PostMapping("/posts")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> createPost(Authentication authentication,
                                                      @RequestBody ListingService.ItemPostCreate request) {
        return Envelope.ok(view(listingService.createDraft(authentication.getName(), request)));
    }

    @PatchMapping("/posts/{postId}")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> updatePost(Authentication authentication,
                                                      @PathVariable String postId,
                                                      @RequestBody ListingService.ItemPostUpdate request) {
        return Envelope.ok(view(listingService.updateDraft(authentication.getName(), postId, request)));
    }

    @PostMapping("/posts/{postId}/publish")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> publishPost(Authentication authentication,
                                                       @PathVariable String postId) {
        return Envelope.ok(view(listingService.publish(authentication.getName(), postId)));
    }

    @PostMapping("/posts/{postId}/close")
    @RequiresAgreement
    public Envelope.Response<ItemPostView> closePost(Authentication authentication,
                                                     @PathVariable String postId) {
        return Envelope.ok(view(listingService.close(authentication.getName(), postId)));
    }

    @GetMapping("/posts/{postId}")
    public Envelope.Response<ItemPostView> getPost(Authentication authentication,
                                                   @PathVariable String postId) {
        return Envelope.ok(view(listingService.getDetail(authentication.getName(), postId)));
    }

    @PostMapping("/posts/{postId}/images")
    @RequiresAgreement
    public Envelope.Response<ItemImageView> uploadImage(Authentication authentication,
                                                        @PathVariable String postId,
                                                        @RequestParam("file") MultipartFile file)
            throws IOException {
        ItemImage image = listingService.uploadImage(
            authentication.getName(), postId, file.getBytes(), file.getContentType());
        return Envelope.ok(ItemImageView.of(image));
    }

    @DeleteMapping("/posts/{postId}/images/{imageId}")
    @RequiresAgreement
    public Envelope.Response<Map<String, Object>> deleteImage(Authentication authentication,
                                                              @PathVariable String postId,
                                                              @PathVariable String imageId) {
        listingService.deleteImage(authentication.getName(), postId, imageId);
        return Envelope.ok(Map.of());
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
        List<ItemPostView> views = result.items().stream().map(this::view).toList();
        ListingService.Pagination p = result.pagination();
        return Envelope.ok(new ItemPostListData(views, p));
    }

    @GetMapping("/me/posts")
    public Envelope.Response<ItemPostListData> listMyPosts(Authentication authentication,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        ListingService.SearchResult result =
            listingService.listMyPosts(authentication.getName(), page, pageSize);
        List<ItemPostView> views = result.items().stream().map(this::view).toList();
        return Envelope.ok(new ItemPostListData(views, result.pagination()));
    }
}
