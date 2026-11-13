package com.lostfound.directory;

/** 校区实体。status: PENDING | ACTIVE | REJECTED。 */
public record Campus(String id, String schoolId, String name, String status, String createdAt) {
}
