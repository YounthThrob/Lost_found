package com.lostfound.operations;

/** 运营管理动作审计事件。metadata 以 reason 字段承载处理原因。 */
public record AuditEvent(String id, String actorId, String eventType, String targetType,
                         String targetId, String reason, String createdAt) {
}
