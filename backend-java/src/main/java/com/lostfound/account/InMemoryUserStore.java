package com.lostfound.account;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存用户存储。
 * 说明：walking skeleton 阶段使用内存存储以保持可运行；后续 change（资料/发布）再引入 JPA 持久化。
 */
@Component
public class InMemoryUserStore {

    private final Map<String, UserAccount> byOpenid = new ConcurrentHashMap<>();

    public UserAccount findByOpenid(String openid) {
        return byOpenid.computeIfAbsent(openid,
            o -> new UserAccount(UUID.randomUUID().toString(), o, "ACTIVE"));
    }

    public UserAccount findById(String id) {
        return byOpenid.values().stream()
            .filter(u -> u.id().equals(id))
            .findFirst()
            .orElse(null);
    }
}
