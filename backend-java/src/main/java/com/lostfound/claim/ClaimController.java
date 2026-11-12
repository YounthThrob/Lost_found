package com.lostfound.claim;

import com.lostfound.account.RequiresAgreement;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    public record ClaimView(String id, String postId, String applicantId, String statement,
                            String status, int version, String createdAt, String updatedAt) {

        static ClaimView from(Claim c) {
            return new ClaimView(c.id(), c.postId(), c.applicantId(), c.statement(),
                c.status(), c.version(), c.createdAt(), c.updatedAt());
        }
    }

    public record ClaimListData(List<ClaimView> items, ClaimService.Pagination pagination) {
    }

    @PostMapping("/posts/{postId}/claims")
    @RequiresAgreement
    public Envelope.Response<ClaimView> createClaim(Authentication authentication,
                                                    @PathVariable String postId,
                                                    @RequestBody ClaimService.ClaimCreate request) {
        return Envelope.ok(ClaimView.from(
            claimService.submit(authentication.getName(), postId, request)));
    }

    @GetMapping("/posts/{postId}/claims")
    public Envelope.Response<ClaimListData> listPostClaims(Authentication authentication,
                                                           @PathVariable String postId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        ClaimService.ClaimListResult result =
            claimService.listByPost(authentication.getName(), postId, page, pageSize);
        List<ClaimView> views = result.items().stream().map(ClaimView::from).toList();
        return Envelope.ok(new ClaimListData(views, result.pagination()));
    }

    @GetMapping("/me/claims")
    public Envelope.Response<ClaimListData> listMyClaims(Authentication authentication,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        ClaimService.ClaimListResult result =
            claimService.listMyClaims(authentication.getName(), page, pageSize);
        List<ClaimView> views = result.items().stream().map(ClaimView::from).toList();
        return Envelope.ok(new ClaimListData(views, result.pagination()));
    }

    @PostMapping("/claims/{claimId}/request-info")
    public Envelope.Response<ClaimView> requestInfo(Authentication authentication,
                                                    @PathVariable String claimId) {
        return Envelope.ok(ClaimView.from(
            claimService.requestInfo(authentication.getName(), claimId)));
    }

    @PostMapping("/claims/{claimId}/accept")
    public Envelope.Response<ClaimView> accept(Authentication authentication,
                                               @PathVariable String claimId) {
        return Envelope.ok(ClaimView.from(
            claimService.accept(authentication.getName(), claimId)));
    }

    @PostMapping("/claims/{claimId}/reject")
    public Envelope.Response<ClaimView> reject(Authentication authentication,
                                               @PathVariable String claimId) {
        return Envelope.ok(ClaimView.from(
            claimService.reject(authentication.getName(), claimId)));
    }
}
