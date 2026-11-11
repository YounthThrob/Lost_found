# Proposal

## Why

后端 MVP 已覆盖账户、发布、认领、沟通、交接、举报、线索、收藏与限流，但 REQ-008「举报、拉黑与运营处理」中的**运营管理动作**尚未落地：目前没有运营身份，无法下架信息（`REMOVED`）、限制账户（`RESTRICTED`），管理动作也没有审计记录。`abuse-prevention` 主 spec 已明确「审计日志与运营触发封禁属后续 change」；§18 验收门槛「举报、拉黑、下架和账号限制能够端到端运行」「运营管理动作具有审计记录」依赖本 change。本 change 落地 `operations-management` capability 的 MVP 切片：运营身份鉴权、下架信息、限制/解除限制账户、查看举报与审计日志。

## What Changes

- 新增运营身份：`User` 增加 `role` 字段（默认 `USER`，运营为 `OPERATOR`）；mock 登录新增 `operator-code → openid_operator_001`。
- 新增 5 个运营端点（`/api/v1/admin/*`）：下架信息、限制账户、解除限制、查看全部举报、查看审计日志。
- 新增信息状态 `REMOVED`（PRODUCT.md §9.1：任意公开状态 → 违规处理成立 → REMOVED）。
- 新增业务错误码 `OPERATOR_REQUIRED`（403）：非运营用户访问管理端点时拒绝。
- 新增 `AuditEvent` 审计记录（PRODUCT.md §10），每个管理动作记录操作者、事件类型、目标、原因与时间。
- Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `operations-management`：运营身份鉴权、下架信息、限制/解除限制账户、查看举报与审计日志、管理动作审计。

### Modified Capabilities

- `user-account`：`User` 增加 `role` 字段（字段增量，行为不变）。
- `lost-found-listing`：信息状态机新增 `REMOVED`（状态枚举增量）。
- `content-moderation`：运营可查看全部举报（新增管理侧查询，用户侧行为不变）。

## Impact

- 修改 `contracts/error-codes.yaml`：新增 `OPERATOR_REQUIRED`（403）。
- 修改 `openapi/product-api.yaml`：`ItemPostView.status` 枚举新增 `REMOVED`；新增 5 个 admin 端点与 `AdminActionRequest`、`UserStatusView`、`UserStatusResponse`、`RemovePostResponse`、`AuditEventView`、`AuditEventListData`、`AuditEventListResponse` schema（举报列表复用 `ReportListResponse`）。
- 修改 `backend-java/` 与 `backend-python/`：`UserAccount` 增 `role`；登录按 mock openid 判定角色；新增 `operations` 包（AuditEvent、AuditStore、OperationsService、OperationsController）；`ListingService` 增 `remove`；`ReportStore` 增 `listAll`。
- 扩展 `contracts/run_contract_tests.py`：运营鉴权、下架、限制/解除、举报列表、审计日志等探针。
