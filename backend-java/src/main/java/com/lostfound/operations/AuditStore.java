package com.lostfound.operations;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存审计事件存储。 */
@Component
public class AuditStore {

    private final Map<String, AuditEvent> byId = new ConcurrentHashMap<>();

    public void save(AuditEvent event) {
        byId.put(event.id(), event);
    }

    public List<AuditEvent> listAll() {
        return byId.values().stream()
            .sorted(Comparator.comparing(AuditEvent::createdAt, Comparator.reverseOrder())
                .thenComparing(AuditEvent::id))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
