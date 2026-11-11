package com.lostfound.clue;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存线索存储。walking skeleton 阶段使用内存，后续 change 再引入 JPA 持久化。 */
@Component
public class ClueStore {

    private final Map<String, Clue> byId = new ConcurrentHashMap<>();

    public void save(Clue clue) {
        byId.put(clue.id(), clue);
    }

    public Clue findById(String id) {
        return byId.get(id);
    }

    /** 按提交时间倒序返回指定信息的线索。 */
    public List<Clue> listByPost(String postId) {
        return byId.values().stream()
            .filter(c -> c.postId().equals(postId))
            .sorted(Comparator.comparing(Clue::createdAt, Comparator.reverseOrder())
                .thenComparing(Clue::id))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
