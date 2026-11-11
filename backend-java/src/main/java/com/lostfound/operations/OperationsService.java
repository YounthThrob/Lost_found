package com.lostfound.operations;

import com.lostfound.account.InMemoryUserStore;
import com.lostfound.account.UserAccount;
import com.lostfound.listing.ItemPost;
import com.lostfound.listing.ListingService;
import com.lostfound.moderation.Report;
import com.lostfound.moderation.ReportStore;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OperationsService {

    public record AdminActionRequest(String reason) {
    }

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record ReportListResult(List<Report> items, Pagination pagination) {
    }

    public record AuditEventListResult(List<AuditEvent> items, Pagination pagination) {
    }

    private static final int MAX_REASON = 200;
    private static final int MAX_PAGE_SIZE = 100;

    private final ListingService listingService;
    private final InMemoryUserStore userStore;
    private final ReportStore reportStore;
    private final AuditStore auditStore;

    public OperationsService(ListingService listingService, InMemoryUserStore userStore,
                             ReportStore reportStore, AuditStore auditStore) {
        this.listingService = listingService;
        this.userStore = userStore;
        this.reportStore = reportStore;
        this.auditStore = auditStore;
    }

    public ItemPost removePost(String operatorId, String postId, String reason) {
        requireReason(reason);
        ItemPost post = listingService.remove(postId);
        audit("POST_REMOVED", "POST", postId, reason, operatorId);
        return post;
    }

    public UserAccount restrictUser(String operatorId, String userId, String reason) {
        requireReason(reason);
        UserAccount user = userStore.restrict(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        audit("USER_RESTRICTED", "USER", userId, reason, operatorId);
        return user;
    }

    public UserAccount unrestrictUser(String operatorId, String userId, String reason) {
        requireReason(reason);
        UserAccount user = userStore.unrestrict(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        audit("USER_UNRESTRICTED", "USER", userId, reason, operatorId);
        return user;
    }

    public ReportListResult listAllReports(int page, int pageSize) {
        validatePage(page, pageSize);
        List<Report> all = reportStore.listAll();
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<Report> items = from >= total ? List.of()
            : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new ReportListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    public AuditEventListResult listAuditEvents(int page, int pageSize) {
        validatePage(page, pageSize);
        List<AuditEvent> all = auditStore.listAll();
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<AuditEvent> items = from >= total ? List.of()
            : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new AuditEventListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    private void audit(String eventType, String targetType, String targetId, String reason,
                       String operatorId) {
        auditStore.save(new AuditEvent(UUID.randomUUID().toString(), operatorId, eventType,
            targetType, targetId, reason.trim(), Instant.now().toString()));
    }

    private void requireReason(String reason) {
        if (reason == null || reason.trim().isEmpty() || reason.trim().length() > MAX_REASON) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
