package com.lostfound.moderation;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存举报存储。 */
@Component
public class ReportStore {

    private final Map<String, Report> byId = new ConcurrentHashMap<>();

    public void save(Report report) {
        byId.put(report.id(), report);
    }

    public List<Report> listByReporter(String reporterId) {
        return byId.values().stream()
            .filter(r -> r.reporterId().equals(reporterId))
            .sorted(Comparator.comparing(Report::createdAt, Comparator.reverseOrder())
                .thenComparing(Report::id))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
