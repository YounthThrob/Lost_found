package com.lostfound.listing;

/** 寻物/招领信息。不可变，字段与 PRODUCT.md §10 ItemPost 对齐（MVP 子集）。 */
public record ItemPost(String id, String authorId, String type, String category, String title,
                       String publicDescription, String schoolId, String schoolName,
                       String campusId, String campusName, String area,
                       String status, String createdAt, String updatedAt) {
}
