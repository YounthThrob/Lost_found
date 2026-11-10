package com.lostfound.moderation;

import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public record ReportView(String id, String targetType, String targetId, String reason,
                             String description, String status, String reporterId, String createdAt) {

        static ReportView from(Report r) {
            return new ReportView(r.id(), r.targetType(), r.targetId(), r.reason(),
                r.description(), r.status(), r.reporterId(), r.createdAt());
        }
    }

    public record ReportListData(List<ReportView> items, ReportService.Pagination pagination) {
    }

    @PostMapping("/reports")
    public Envelope.Response<ReportView> createReport(Authentication authentication,
                                                      @RequestBody ReportService.ReportCreate request) {
        return Envelope.ok(ReportView.from(reportService.createReport(authentication.getName(), request)));
    }

    @GetMapping("/reports/me")
    public Envelope.Response<ReportListData> listMyReports(Authentication authentication,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        ReportService.ReportListResult result =
            reportService.listMyReports(authentication.getName(), page, pageSize);
        List<ReportView> views = result.items().stream().map(ReportView::from).toList();
        return Envelope.ok(new ReportListData(views, result.pagination()));
    }
}
