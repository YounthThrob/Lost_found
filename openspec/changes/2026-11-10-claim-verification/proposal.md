# Proposal

## Why

C04/C05 已落地寻物/招领的发布、搜索与内容检测，但「认领」闭环尚未建立——拾获者发布招领后无法接收与核验认领申请，失主也无法在线提交归属证明。本 change 落地 `claim-verification` capability（PRODUCT.md §17 顺序 5）的 MVP 切片：失主对展示中的招领信息提交认领申请，拾获者查看、请求补充、接受或拒绝申请，并在服务端校验申请与信息两条状态机的联动。交接安排（PENDING_HANDOVER 之后的 COMPLETED/终止）与安全沟通属于后续 `item-handover`、`safe-communication` capability，不在本 change。

## What Changes

- 新增认领申请资源与状态机：`SUBMITTED → NEEDS_MORE_INFO → ACCEPTED`，终态 `REJECTED`（MVP 子集，WITHDRAWN/CANCELLED/BLOCKED_BY_MODERATION 后补）。
- 新增端点：`POST /api/v1/posts/{postId}/claims`（提交申请）、`GET /api/v1/posts/{postId}/claims`（发布者查看收到的申请）、`POST /api/v1/claims/{claimId}/request-info`、`/accept`、`/reject`。
- 信息状态联动：招领信息在首个申请到达时 `PUBLISHED → VERIFYING`，接受申请时 `VERIFYING → PENDING_HANDOVER`，全部申请关闭后回到 `PUBLISHED`；`VERIFYING` 仍公开可见。
- 新增业务错误码 `CLAIM_DUPLICATE`、`POST_NOT_CLAIMABLE`、`HANDOVER_CONFLICT`（均 409）。
- 隐私：申请 `statement` 与申请人标识仅对申请人、发布者可见，不得进入公开搜索、详情或普通日志。
- 同步更新 OpenAPI 契约，Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `claim-verification`: 认领申请提交、发布者查看、请求补充、接受与拒绝。

### Modified Capabilities

- `lost-found-listing`: 信息状态枚举新增 `VERIFYING`、`PENDING_HANDOVER`；公开可见范围由「仅 PUBLISHED」扩展为「PUBLISHED 或 VERIFYING」。

## Impact

- 修改 `contracts/error-codes.yaml`：新增 `CLAIM_DUPLICATE`、`POST_NOT_CLAIMABLE`、`HANDOVER_CONFLICT`。
- 修改 `openapi/product-api.yaml`：`ItemPostView.status` 枚举加入 `VERIFYING`、`PENDING_HANDOVER`；新增 5 个 claim 端点与 `ClaimCreate`/`ClaimView`/`ClaimListData`/`ClaimResponse`/`ClaimListResponse` schema。
- 修改 `backend-java/` 与 `backend-python/`：listing 可见性规则扩展、新增 claim 模块（Claim store/service/controller）与测试。
- 扩展 `contracts/run_contract_tests.py`：提交/重复/不可认领/发布者查看/非发布者越权/请求补充/接受/拒绝/信息状态联动/待交接冲突。
