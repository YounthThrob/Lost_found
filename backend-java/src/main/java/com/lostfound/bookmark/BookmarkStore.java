package com.lostfound.bookmark;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存收藏存储。walking skeleton 阶段使用内存，后续 change 再引入 JPA 持久化。 */
@Component
public class BookmarkStore {

    private final Map<String, Bookmark> byKey = new ConcurrentHashMap<>();

    private static String key(String userId, String postId) {
        return userId + ":" + postId;
    }

    public Bookmark find(String userId, String postId) {
        return byKey.get(key(userId, postId));
    }

    public void save(Bookmark bookmark) {
        byKey.put(key(bookmark.userId(), bookmark.postId()), bookmark);
    }

    public void remove(String userId, String postId) {
        byKey.remove(key(userId, postId));
    }

    /** 返回某用户全部收藏，按收藏时间倒序。 */
    public List<Bookmark> listByUser(String userId) {
        return byKey.values().stream()
            .filter(b -> b.userId().equals(userId))
            .sorted(Comparator.comparing(Bookmark::createdAt, Comparator.reverseOrder())
                .thenComparing(Bookmark::postId))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byKey.clear();
    }
}
