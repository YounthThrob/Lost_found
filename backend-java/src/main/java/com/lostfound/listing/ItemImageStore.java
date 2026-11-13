package com.lostfound.listing;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存图片元数据存储。walking skeleton 阶段使用内存，后续 change 再引入 JPA 持久化。 */
@Component
public class ItemImageStore {

    private final Map<String, ItemImage> byId = new ConcurrentHashMap<>();

    public ItemImage findById(String id) {
        return byId.get(id);
    }

    public void save(ItemImage image) {
        byId.put(image.id(), image);
    }

    public List<ItemImage> listByPost(String postId) {
        return byId.values().stream()
            .filter(i -> i.postId().equals(postId))
            .sorted(Comparator.comparingInt(ItemImage::sortOrder).thenComparing(ItemImage::id))
            .toList();
    }

    public void delete(String id) {
        byId.remove(id);
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
