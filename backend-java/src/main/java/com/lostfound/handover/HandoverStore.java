package com.lostfound.handover;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存交接安排存储。 */
@Component
public class HandoverStore {

    private final Map<String, Handover> byId = new ConcurrentHashMap<>();

    public void save(Handover handover) {
        byId.put(handover.id(), handover);
    }

    public Handover findById(String id) {
        return byId.get(id);
    }

    /** 同一申请至多一个交接安排。 */
    public Handover findByClaimId(String claimId) {
        return byId.values().stream()
            .filter(h -> h.claimId().equals(claimId))
            .findFirst()
            .orElse(null);
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
