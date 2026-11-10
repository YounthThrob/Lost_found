package com.lostfound.communication;

/** 拉黑关系。不可变，字段与 PRODUCT.md §10 BlockRelation 对齐。 */
public record BlockRelation(String blockerId, String blockedId, String createdAt) {
}
