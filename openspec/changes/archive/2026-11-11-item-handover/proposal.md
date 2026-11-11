# Proposal

## Why

C06 已让拾获者接受认领申请，使信息进入 `PENDING_HANDOVER`；C06 之后 `safe-communication` 提供了受控沟通与拉黑。但「交接」本身尚未闭环——双方无法约定时间地点、分别确认完成，也无法终止交接。本 change 落地 `item-handover` capability（PRODUCT.md §17 顺序 7）的 MVP 切片：为已接受申请创建交接安排，双方分别确认后完成，或任一方终止。

## What Changes

- 新增交接资源与状态机：`Handover`（id/claimId/proposerId/timeWindow/publicPlace/ownerConfirmedAt/claimantConfirmedAt/status），`PENDING → COMPLETED`（双方确认）或 `PENDING → CANCELLED`（终止）。
- 新增端点：`POST /api/v1/handovers`（创建交接安排）、`POST /api/v1/handovers/{id}/confirm`（当前用户确认）、`POST /api/v1/handovers/{id}/cancel`（终止交接）。
- 状态联动：双方均确认后信息 `PENDING_HANDOVER → COMPLETED`、申请 `ACCEPTED → COMPLETED`；终止后信息 `PENDING_HANDOVER → PUBLISHED`、申请 `ACCEPTED → CANCELLED`。
- 新增业务错误码 `HANDOVER_ALREADY_EXISTS`（409）。
- 隐私：交接的时间/地点仅对交接双方（发布者与申请人）可见，绝不进入公开接口、普通日志或搜索索引。
- 同步更新 OpenAPI 契约，Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `item-handover`: 交接安排创建、双方确认完成、终止交接，及交接与信息/申请状态的联动。

### Modified Capabilities

- 无。信息 `COMPLETED` 与申请 `COMPLETED`/`CANCELLED` 为新增终态，其可见性（非公开，仅作者可见）已被 `lost-found-listing` 既有可见性规则覆盖。

## Impact

- 修改 `contracts/error-codes.yaml`：新增 `HANDOVER_ALREADY_EXISTS`（409）。
- 修改 `openapi/product-api.yaml`：`ItemPostView.status` 加入 `COMPLETED`；`ClaimView.status` 加入 `COMPLETED`、`CANCELLED`；新增 3 个 handover 端点与 `HandoverCreate`/`HandoverView`/`HandoverResponse` schema。
- 修改 `backend-java/` 与 `backend-python/`：新增 handover 模块（store/service/controller），`ErrorCode` 新增 `HANDOVER_ALREADY_EXISTS`。
- 扩展 `contracts/run_contract_tests.py`：创建/重复创建/非参与者/未接受/单方确认/双方确认/终止/非法状态/信息与申请状态联动/字段校验。
