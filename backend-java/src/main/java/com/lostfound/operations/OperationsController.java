package com.lostfound.operations;

import com.lostfound.account.UserAccount;
import com.lostfound.listing.ListingController;
import com.lostfound.moderation.ReportController;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class OperationsController {

    private final OperationsService service;
    private final OperatorGuard guard;

    public OperationsController(OperationsService service, OperatorGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    public record UserStatusView(String id, String status) {
        static UserStatusView from(UserAccount u) {
            return new UserStatusView(u.id(), u.status());
        }
    }

    public record AuditEventView(String id, String actorId, String eventType, String targetType,
                                 String targetId, Map<String, Object> metadata, String createdAt) {
        static AuditEventView from(AuditEvent e) {
            return new AuditEventView(e.id(), e.actorId(), e.eventType(), e.targetType(),
                e.targetId(), Map.of("reason", e.reason()), e.createdAt());
        }
    }

    public record AuditEventListData(List<AuditEventView> items, OperationsService.Pagination pagination) {
    }

    public record ReportListData(List<ReportController.ReportView> items,
                                 OperationsService.Pagination pagination) {
    }

    @PostMapping("/admin/posts/{postId}/remove")
    public Envelope.Response<ListingController.ItemPostView> removePost(
            Authentication authentication,
            @PathVariable String postId,
            @RequestBody OperationsService.AdminActionRequest request) {
        guard.requireOperator(authentication.getName());
        return Envelope.ok(ListingController.ItemPostView.from(
            service.removePost(authentication.getName(), postId, request.reason())));
    }

    @PostMapping("/admin/users/{userId}/restrict")
    public Envelope.Response<UserStatusView> restrictUser(
            Authentication authentication,
            @PathVariable String userId,
            @RequestBody OperationsService.AdminActionRequest request) {
        guard.requireOperator(authentication.getName());
        return Envelope.ok(UserStatusView.from(
            service.restrictUser(authentication.getName(), userId, request.reason())));
    }

    @PostMapping("/admin/users/{userId}/unrestrict")
    public Envelope.Response<UserStatusView> unrestrictUser(
            Authentication authentication,
            @PathVariable String userId,
            @RequestBody OperationsService.AdminActionRequest request) {
        guard.requireOperator(authentication.getName());
        return Envelope.ok(UserStatusView.from(
            service.unrestrictUser(authentication.getName(), userId, request.reason())));
    }

    @GetMapping("/admin/reports")
    public Envelope.Response<ReportListData> listAllReports(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        guard.requireOperator(authentication.getName());
        OperationsService.ReportListResult result = service.listAllReports(page, pageSize);
        List<ReportController.ReportView> views = result.items().stream()
            .map(ReportController.ReportView::from).toList();
        return Envelope.ok(new ReportListData(views, result.pagination()));
    }

    @GetMapping("/admin/audit-events")
    public Envelope.Response<AuditEventListData> listAuditEvents(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        guard.requireOperator(authentication.getName());
        OperationsService.AuditEventListResult result = service.listAuditEvents(page, pageSize);
        List<AuditEventView> views = result.items().stream().map(AuditEventView::from).toList();
        return Envelope.ok(new AuditEventListData(views, result.pagination()));
    }
}
