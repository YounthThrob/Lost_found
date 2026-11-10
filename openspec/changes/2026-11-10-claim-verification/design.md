# Design

## Context

- 已有 `lost-found-listing`（DRAFT→PENDING_REVIEW→PUBLISHED/REJECTED→CLOSED）、`content-moderation`（发布检测 + 举报）与 `user-account`（登录/协议）。
- PRODUCT.md §9.1：`PUBLISHED → VERIFYING`（有申请进入核验）、`VERIFYING → PENDING_HANDOVER`（一个申请通过）、`VERIFYING → PUBLISHED`（全部申请关闭）。
- PRODUCT.md §9.2：认领申请状态机 `SUBMITTED → NEEDS_MORE_INFO → ACCEPTED → PENDING_HANDOVER → COMPLETED`，其他终态 REJECTED/WITHDRAWN/CANCELLED/BLOCKED_BY_MODERATION。
- REQ-004/REQ-005 与 §11 认领端点；§11.2 认领相关错误码。

## Goals / Non-Goals

**Goals:**

- 失主（申请人）对展示中的招领（FOUND）信息提交认领申请，附非空 `statement` 归属证明。
- 拾获者（发布者）查看收到的申请、请求补充、接受或拒绝。
- 服务端校验申请状态机与信息状态联动，杜绝客户端指定目标状态。
- 申请内容仅对申请人与发布者可见。

**Non-Goals:**

- 不实现交接安排与双方确认（PENDING_HANDOVER 之后），属 `item-handover`。
- 不实现申请撤回（WITHDRAWN）、补充内容回填与版本递增的完整审计、审核拦截（BLOCKED_BY_MODERATION）。
- 不实现认领频率限制（`CLAIM_RATE_LIMITED`），属 `abuse-prevention`。
- 不实现不公开核验特征（PrivateClue）及其应用层加密，属后续「私密核验特征」change。
- 不实现「我的申请」列表与订阅消息通知，申请状态暂以操作响应回显。

## Decisions

1. **申请模型**：`Claim` 字段 id/postId/applicantId/statement/status/version/createdAt/updatedAt。`statement` 非空 ≤2000；`version` 初值 1（为后续「补充」预留）；`status` ∈ {SUBMITTED, NEEDS_MORE_INFO, ACCEPTED, REJECTED}。

2. **可认领判定**：信息 `type == FOUND` 且 `status ∈ {PUBLISHED, VERIFYING}` 且非作者本人，否则 `POST_NOT_CLAIMABLE`（信息不存在则为 `NOT_FOUND`）。同一申请人同一信息仅一个进行中申请（SUBMITTED/NEEDS_MORE_INFO/ACCEPTED），重复提交 `CLAIM_DUPLICATE`。

3. **状态机与信息联动**：
   - 提交：首个申请到达时信息 `PUBLISHED → VERIFYING`。
   - 请求补充：仅发布者、仅 `SUBMITTED → NEEDS_MORE_INFO`。
   - 接受：仅发布者、`SUBMITTED/NEEDS_MORE_INFO → ACCEPTED`；若同信息已有 `ACCEPTED` 申请则 `HANDOVER_CONFLICT`；信息 `VERIFYING → PENDING_HANDOVER`。
   - 拒绝：仅发布者、`SUBMITTED/NEEDS_MORE_INFO → REJECTED`；若信息为 `VERIFYING` 且无其他进行中申请则回到 `PUBLISHED`。
   - 非法迁移一律 `INVALID_STATE_TRANSITION`。

4. **公开可见性**：`PUBLISHED` 与 `VERIFYING` 公开可见（`PENDING_HANDOVER` 起不再公开，交由交接流程）。搜索与详情可见性规则同步扩展。

5. **鉴权**：提交申请要求协议确认（与「发布/留言/认领」同级）；查看/补充/接受/拒绝要求有效会话 + 发布者身份校验（非作者 `NOT_FOUND`）。

6. **隐私**：`statement` 与 `applicantId`（UUID，非 openid）仅出现在申请人或发布者可见的响应中，绝不进入公开搜索/详情；内存存储不落日志。

## Risks / Trade-offs

- [状态联动耦合] 申请生命周期横跨信息状态，需在 claim 服务内以 post store 做乐观读写；MVP 内存单机，无并发竞态问题，正式持久化后再引入乐观锁版本。
- [无通知/我的申请列表] 申请人提交后只能靠操作响应或后续通知获知进展，MVP 可接受，通知属后续 change。
- [未加密 statement] 正式存储与 PrivateClue/ClaimEvidence 加密随「私密核验特征」change 落地；MVP 以访问控制兜底隐私红线。

## Migration Plan

- 无既有数据迁移（内存存储）。新增错误码与 claim schema 为契约增量。
