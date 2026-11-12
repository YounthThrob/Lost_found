package com.lostfound.listing;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存信息存储。walking skeleton 阶段使用内存，后续 change 再引入 JPA 持久化。 */
@Component
public class ItemPostStore {

    private final Map<String, ItemPost> byId = new ConcurrentHashMap<>();

    public ItemPost findById(String id) {
        return byId.get(id);
    }

    public void save(ItemPost post) {
        byId.put(post.id(), post);
    }

    /** 仅返回公开可见信息（PUBLISHED 或 VERIFYING），按创建时间倒序。 */
    public List<ItemPost> listVisible() {
        return byId.values().stream()
            .filter(p -> "PUBLISHED".equals(p.status()) || "VERIFYING".equals(p.status()))
            .sorted(Comparator.comparing(ItemPost::createdAt, Comparator.reverseOrder())
                .thenComparing(ItemPost::id))
            .toList();
    }

    /** 返回某作者的全部信息（不分状态），按创建时间倒序。 */
    public List<ItemPost> listByAuthor(String authorId) {
        return byId.values().stream()
            .filter(p -> p.authorId().equals(authorId))
            .sorted(Comparator.comparing(ItemPost::createdAt, Comparator.reverseOrder())
                .thenComparing(ItemPost::id))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
