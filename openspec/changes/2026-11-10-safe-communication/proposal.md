# Proposal

## Why

C05/C06 已落地内容检测与认领申请核验，申请被接受后进入待交接，但拾获者与失主之间尚无受控的沟通渠道，也无法将骚扰用户拉黑。本 change 落地 `safe-communication` capability（PRODUCT.md §17 顺序 6）的 MVP 切片：已接受的申请双方可交换有限消息（结构化消息 + 必要文本留言），用户可拉黑对方并在被拉黑后无法继续发送消息。交接安排（时间/地点/双方确认）属后续 `item-handover`，不在本 change。

## What Changes

- 新增拉黑资源：`POST /api/v1/blocks`（拉黑）、`DELETE /api/v1/blocks/{userId}`（解除拉黑）、`GET /api/v1/me/blocks`（查看黑名单），拉黑幂等、不可拉黑自己或不存在用户。
- 新增有限消息：`POST /api/v1/conversations/{conversationId}/messages`（发送）、`GET /api/v1/conversations/{conversationId}/messages`（查看）。MVP 下 `conversationId` 即已接受认领申请的 `claimId`，参与者为信息发布者与申请人。
- 消息类型：`TEXT`（文本留言）与结构化消息 `SUGGEST_TIME`/`SUGGEST_PLACE`/`LATE_REMINDER`；`TERMINATE_HANDOVER` 属交接流程，`REQUEST_INFO` 已由认领核验端点覆盖，均不在本 change。
- 骚扰控制：被对方拉黑后继续发送消息返回新业务错误码 `BLOCKED`（HTTP 403）。
- 隐私：消息仅对会话双方可见；消息 `senderId` 为不可预测 UUID 而非 openid，绝不进入公开接口、普通日志或搜索索引。
- 同步更新 OpenAPI 契约，Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `safe-communication`: 有限消息（结构化 + 文本）、拉黑/解除拉黑/黑名单、骚扰控制（BLOCKED）。

### Modified Capabilities

- 无。消息不改变信息或申请状态机；交接状态迁移属 `item-handover`。

## Impact

- 修改 `contracts/error-codes.yaml`：新增 `BLOCKED`（403）。
- 修改 `openapi/product-api.yaml`：新增 block 与 conversation 端点及 `BlockCreate`/`BlockView`/`BlockListData`/`BlockResponse`/`BlockListResponse`/`MessageCreate`/`MessageView`/`MessageListData`/`MessageResponse`/`MessageListResponse` schema。
- 修改 `backend-java/` 与 `backend-python/`：新增 communication 模块（block + message store/service/controller）、`ErrorCode.BLOCKED`、应用入口注册路由。
- 扩展 `contracts/run_contract_tests.py`：发送消息/查看消息/拉黑/重复拉黑/拉黑自己/拉黑不存在用户/黑名单/被拉黑后发送/解除拉黑/越权查看/未接受不可发送/非法类型/空内容。
