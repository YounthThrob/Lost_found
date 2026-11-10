package com.lostfound.claim;

/** 认领申请。字段与 PRODUCT.md §10 Claim 对齐（MVP 子集）。 */
public record Claim(String id, String postId, String applicantId, String statement,
                    String status, int version, String createdAt, String updatedAt) {
}
