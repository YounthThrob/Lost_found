package com.lostfound.communication;

/** 有限消息。不可变，字段与 PRODUCT.md §10 Message 对齐（MVP 子集）。 */
public record Message(String id, String conversationId, String senderId, String type,
                      String content, String createdAt) {
}
