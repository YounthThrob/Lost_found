package com.lostfound.handover;

/** 交接安排。字段与 PRODUCT.md §10 Handover 对齐（MVP 子集）。 */
public record Handover(String id, String claimId, String proposerId, String timeWindow,
                       String publicPlace, String ownerConfirmedAt, String claimantConfirmedAt,
                       String status, String createdAt) {
}
