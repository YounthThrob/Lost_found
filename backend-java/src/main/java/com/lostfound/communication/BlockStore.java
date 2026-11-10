package com.lostfound.communication;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存拉黑关系存储。 */
@Component
public class BlockStore {

    private final Map<String, Map<String, BlockRelation>> byBlocker = new ConcurrentHashMap<>();

    public BlockRelation find(String blockerId, String blockedId) {
        Map<String, BlockRelation> inner = byBlocker.get(blockerId);
        return inner == null ? null : inner.get(blockedId);
    }

    public BlockRelation put(BlockRelation relation) {
        byBlocker.computeIfAbsent(relation.blockerId(), k -> new ConcurrentHashMap<>())
            .put(relation.blockedId(), relation);
        return relation;
    }

    public void remove(String blockerId, String blockedId) {
        Map<String, BlockRelation> inner = byBlocker.get(blockerId);
        if (inner != null) {
            inner.remove(blockedId);
        }
    }

    public List<BlockRelation> listByBlocker(String blockerId) {
        Map<String, BlockRelation> inner = byBlocker.get(blockerId);
        if (inner == null) {
            return List.of();
        }
        return inner.values().stream()
            .sorted(Comparator.comparing(BlockRelation::createdAt, Comparator.reverseOrder())
                .thenComparing(BlockRelation::blockedId))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byBlocker.clear();
    }
}
