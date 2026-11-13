package com.lostfound.listing;

/** 信息图片元数据。数据库仅保存 storage_key，二进制交存储抽象落地。 */
public record ItemImage(String id, String postId, String storageKey, int sortOrder, String moderationStatus) {
}
