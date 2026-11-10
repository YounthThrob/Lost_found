package com.lostfound.moderation;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    public record ReportCreate(String targetType, String targetId, String reason, String description) {
    }

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record ReportListResult(List<Report> items, Pagination pagination) {
    }

    private static final int MAX_REASON = 100;
    private static final int MAX_DESCRIPTION = 500;
    private static final int MAX_ID = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportStore store;

    public ReportService(ReportStore store) {
        this.store = store;
    }

    public Report createReport(String reporterId, ReportCreate request) {
        String targetType = request.targetType() == null ? null : request.targetType().trim();
        if (!"POST".equals(targetType) && !"USER".equals(targetType)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        requireText(request.targetId(), MAX_ID);
        requireText(request.reason(), MAX_REASON);
        String description = normalize(request.description());
        if (description != null && description.length() > MAX_DESCRIPTION) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        Report report = new Report(UUID.randomUUID().toString(), reporterId, targetType,
            request.targetId().trim(), request.reason().trim(), description, "SUBMITTED",
            Instant.now().toString());
        store.save(report);
        return report;
    }

    public ReportListResult listMyReports(String reporterId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<Report> all = store.listByReporter(reporterId);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<Report> items = from >= total ? List.of() : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new ReportListResult(items, new Pagination(page, pageSize, total, hasMore));
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
}
