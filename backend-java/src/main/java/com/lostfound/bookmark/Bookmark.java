package com.lostfound.bookmark;

/** 收藏关系。不可变，字段与 PRODUCT.md §10 Bookmark 对齐（user_id、post_id、created_at 联合唯一）。 */
public record Bookmark(String userId, String postId, String createdAt) {
}
