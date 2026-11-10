# Proposal

## Why

用户协议与风险提示确认是账户能力的第二块地基：它决定一个已登录用户能否进入「发布、认领、留言」这些真正产生互助价值的受限操作。REQ-001 明确「首次使用必须确认用户协议、隐私说明及非官方平台提示；未确认协议则禁止发布/留言/认领」。本 change 在既有 `user-account` capability 上补充协议确认状态、确认接口与受限操作门控，兑现「非官方边界」与「首次确认」约束。

## What Changes

- 账户新增协议确认状态 `agreementAccepted`（新账户默认未确认），登录响应与 `GET /api/v1/me` 均返回该状态，供客户端决定是否展示协议页。
- 新增确认接口 `POST /api/v1/me/agreement`：确认用户协议、隐私说明与非官方平台提示，幂等。
- 新增错误码 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）：未确认协议时禁止执行受限操作（发布/认领/留言）。
- 落地可复用的协议门控组件：Java `@RequiresAgreement` 注解 + 拦截器，Python `require_agreement` 依赖；后续受限端点（C06 发布、C14 认领、C16 留言）接入时直接复用。
- 同步更新 OpenAPI 契约与错误码表（共同事实源）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `user-account`：新增协议确认状态、确认接口与受限操作门控规则。

## Impact

- 修改 `openapi/product-api.yaml`：`LoginResponse`/`MeResponse` 增加 `agreementAccepted`，新增 `POST /me/agreement` 端点与响应结构。
- 修改 `contracts/error-codes.yaml`：新增 `AGREEMENT_NOT_ACCEPTED`（403）。
- 修改 `backend-java/` 与 `backend-python/`：账户状态、确认接口、门控组件与测试。
- 受限操作端点（发布/认领/留言）尚未实现，本 change 仅落地门控机制与规则，端点接入随 C06/C14/C16 完成。
