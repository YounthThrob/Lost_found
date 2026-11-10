# Design

## Context

- 已有 `lost-found-listing`（发布 DRAFT→PENDING_REVIEW→PUBLISHED→CLOSED）与 `user-account`（登录/协议）。
- PRODUCT.md §9.1：PENDING_REVIEW → PUBLISHED（通过）/ REJECTED（审核拒绝）；REQ-008 举报。
- 当前无运营角色鉴权，管理动作（下架/禁言/封禁）不在本 change。

## Goals / Non-Goals

**Goals:**

- 发布时对公开内容（标题 + 公开描述）做确定性违禁词检测，命中则 REJECTED、否则 PUBLISHED。
- 提供举报提交与「我的举报」分页查询。
- 落地 `contracts/moderation-rules.yaml` 违禁词表，两后端一致。

**Non-Goals:**

- 不实现真实内容审核（第三方服务/模型），MVP 用确定性关键词匹配占位。
- 不实现运营管理动作（下架 REMOVED、禁言、封禁），需运营角色鉴权另立 change。
- 不校验举报对象是否存在（避免跨模块耦合），仅记录举报。
- 不处理举报（运营查看/处理举报），另立 change。

## Decisions

1. **检测器**：`ContentModerator` 对 `title` 与 `publicDescription` 做违禁词包含匹配（大小写不敏感），命中任一即 REJECTED。违禁词表 `["转账", "刷单", "诈骗"]` 落 `contracts/moderation-rules.yaml`，两后端以常量镜像并注释指向契约。

2. **状态机**：`publish` 流程 DRAFT → PENDING_REVIEW →（检测）→ PUBLISHED 或 REJECTED。REJECTED 为 MVP 终态（不实现重新提交）。REJECTED 的可见性复用 getDetail 规则（非 PUBLISHED 仅作者可见）。

3. **举报模型**：`Report` 字段 id/reporterId/targetType/targetId/reason/description/status/createdAt。targetType ∈ {POST, USER}，reason 非空 ≤100，description 可选 ≤500，status 恒为 SUBMITTED。不校验 target 存在性。

4. **鉴权与门控**：举报提交与查询要求有效会话（不要求协议确认，保持举报低门槛）。发布仍要求协议确认。

5. **分页**：`GET /reports/me` 复用 `{items, pagination}` 分页，默认按 createdAt 倒序。

## Risks / Trade-offs

- [关键词匹配有误报] → MVP 占位检测，文档明确「真实审核另立 change」；违禁词表为事实源，便于替换。
- [不校验举报对象存在] → 可能产生无效举报，MVP 可接受；运营处理 change 再补校验。

## Migration Plan

- 无既有数据迁移（内存存储）。违禁词表为新增契约文件。
