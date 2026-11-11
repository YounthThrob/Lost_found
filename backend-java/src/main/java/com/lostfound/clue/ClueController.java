package com.lostfound.clue;

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
public class ClueController {

    private final ClueService clueService;

    public ClueController(ClueService clueService) {
        this.clueService = clueService;
    }

    public record ClueView(String id, String postId, String providerId, String content,
                           String status, String createdAt) {

        static ClueView from(Clue c) {
            return new ClueView(c.id(), c.postId(), c.providerId(), c.content(),
                c.status(), c.createdAt());
        }
    }

    public record ClueListData(List<ClueView> items, ClueService.Pagination pagination) {
    }

    @PostMapping("/posts/{postId}/clues")
    @RequiresAgreement
    public Envelope.Response<ClueView> createClue(Authentication authentication,
                                                  @PathVariable String postId,
                                                  @RequestBody ClueService.ClueCreate request) {
        return Envelope.ok(ClueView.from(
            clueService.submit(authentication.getName(), postId, request)));
    }

    @GetMapping("/posts/{postId}/clues")
    public Envelope.Response<ClueListData> listPostClues(Authentication authentication,
                                                         @PathVariable String postId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize) {
        ClueService.ClueListResult result =
            clueService.listPendingByPost(authentication.getName(), postId, page, pageSize);
        List<ClueView> views = result.items().stream().map(ClueView::from).toList();
        return Envelope.ok(new ClueListData(views, result.pagination()));
    }

    @PostMapping("/clues/{clueId}/decide")
    public Envelope.Response<ClueView> decide(Authentication authentication,
                                              @PathVariable String clueId,
                                              @RequestBody ClueService.ClueDecide request) {
        return Envelope.ok(ClueView.from(
            clueService.decide(authentication.getName(), clueId, request)));
    }
}
