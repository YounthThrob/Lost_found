# Design

## Context

- 已有 `claim-verification`（SUBMITTED→NEEDS_MORE_INFO→ACCEPTED/REJECTED，接受后信息 PENDING_HANDOVER）、`content-moderation`（举报 targetType ∈ {POST, USER}）、`user-account`（登录/协议）。
- PRODUCT.md REQ-006：有限沟通默认结构化消息（请求补充/建议时间/建议地点/迟到提醒/终止交接），文本留言需审核/举报/拉黑，被拉黑后不得继续发送消息或申请。
- PRODUCT.md §10：`Message`（id/conversation_id/sender_id/type/content/status/created_at）、`BlockRelation`（blocker_id/blocked_id/created_at）。
- PRODUCT.md §11：`POST /api/v1/blocks`、`DELETE /api/v1/blocks/{userId}`、`GET/POST /api/v1/conversations/{id}/messages`；§11.2 错误码 `BLOCKED`（403）。

## Goals / Non-Goals

**Goals:**

- 用户拉黑/解除拉黑/查看黑名单，拉黑幂等。
- 已接受申请的信息发布者与申请人之间交换有限消息（结构化 + 文本）。
- 服务端校验会话参与者身份、申请已接受状态、消息类型与内容。
- 被拉黑后发送消息被拒绝（`BLOCKED`）。
- 消息内容仅对会话双方可见。

**Non-Goals:**

- 不实现交接安排与双方确认（时间/地点/状态），属 `item-handover`。
- 不实现消息内容审核与消息级举报，文本内容审核随 `abuse-prevention` 或后续切片落地（`content-moderation` 已支持 USER 级举报）。
- 不实现「被拉黑后不得申请」的认领提交拦截，本 change 仅在消息发送路径强制 `BLOCKED`。
- 不实现消息已读回执、删除/撤回、订阅消息通知。
- 不实现自由文本之外的富媒体消息（图片/语音）。

## Decisions

1. **拉黑模型**：`BlockRelation(blockerId, blockedId, createdAt)`，`(blockerId, blockedId)` 唯一。拉黑幂等（重复拉黑返回已有关系）；解除拉黑幂等；不可拉黑自己（`VALIDATION_FAILED`）、不可拉黑不存在用户（`NOT_FOUND`）。`blockedUserId` 为内部用户 UUID。

2. **会话标识**：MVP 下 `conversationId` 复用 `claimId`，不再引入独立 Conversation 实体；参与者由申请推导——信息发布者（post.authorId）与申请人（claim.applicantId）。消息 `conversationId` 即 `claimId`。

3. **消息模型**：`Message(id, conversationId, senderId, type, content, createdAt)`。`type` ∈ {TEXT, SUGGEST_TIME, SUGGEST_PLACE, LATE_REMINDER}；`content` 非空 ≤2000（所有类型统一要求非空内容）。

4. **发送校验顺序**：会话（申请）不存在 → `NOT_FOUND`；发送者非参与者 → `NOT_FOUND`；申请非 `ACCEPTED` → `INVALID_STATE_TRANSITION`；对方已拉黑发送者 → `BLOCKED`；类型/内容非法 → `VALIDATION_FAILED`。

5. **鉴权**：发送消息要求协议确认（属「留言」，`@RequiresAgreement` / `require_agreement`）；查看消息、拉黑/解除/黑名单要求有效会话，不要求协议确认（拉黑属自我保护动作）。

6. **隐私**：消息仅参与者可见；`senderId` 为 UUID；黑名单仅拉黑者本人可见；内存存储不落日志。

## Risks / Trade-offs

- [会话与申请耦合] `conversationId == claimId` 使会话无法脱离申请独立存在；交接多轮会话复用同一申请会话，MVP 可接受，正式版可引入独立 Conversation 实体与多会话支持。
- [仅消息路径强制拉黑] 「被拉黑后不得申请」未在认领提交路径强制，属 `abuse-prevention` 或后续切片补齐；本 change 先落地消息路径的 `BLOCKED`。
- [无消息内容审核] 文本留言暂不经过内容检测，MVP 以拉黑 + 举报（USER 级）兜底，文本审核随后续切片落地。
- [无持久化] 内存存储，重启即失；正式持久化随持久化 change 落地。

## Migration Plan

- 无既有数据迁移（内存存储）。新增 `BLOCKED` 错误码与 block/message schema 为契约增量。
