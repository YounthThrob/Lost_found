package com.lostfound.communication;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 内存消息存储。 */
@Component
public class MessageStore {

    private final Map<String, Message> byId = new ConcurrentHashMap<>();

    public void save(Message message) {
        byId.put(message.id(), message);
    }

    public List<Message> listByConversation(String conversationId) {
        return byId.values().stream()
            .filter(m -> m.conversationId().equals(conversationId))
            .sorted(Comparator.comparing(Message::createdAt, Comparator.reverseOrder())
                .thenComparing(Message::id))
            .toList();
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        byId.clear();
    }
}
