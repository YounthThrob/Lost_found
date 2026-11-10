package com.lostfound.moderation;

/** 举报。字段与 PRODUCT.md §10 Report 对齐（MVP 子集）。 */
public record Report(String id, String reporterId, String targetType, String targetId,
                     String reason, String description, String status, String createdAt) {
}
