package com.lostfound.clue;

/** 线索。字段与 PRODUCT.md §10 Clue 对齐（MVP 子集，纯文本无图片）。 */
public record Clue(String id, String postId, String providerId, String content,
                   String status, String createdAt) {
}
