# Proposal

## Why

微信登录是整条产品链路的地基：发布、认领、沟通、交接等所有能力都依赖一个已登录的平台账户。先跑通登录，可同时建立 walking skeleton，验证 Java 与 Python 两个后端能对同一份 OpenAPI 契约返回一致结果，为后续所有 change 立下「契约优先、双后端等价」的工程基线。

## What Changes

- 新增微信登录端点 `POST /api/v1/session/wechat`：小程序以 `wx.login` 获取的临时 `code` 换取平台会话。
- 新增会话建立与校验：受保护接口要求有效会话，未登录或会话过期返回对应业务错误码。
- 落地双后端契约：在 `openapi/product-api.yaml` 中定义登录端点与响应结构，`backend-java` 与 `backend-python` 分别实现等价行为。
- 建立项目骨架：初始化 `openapi/`、`contracts/`、`backend-java/`、`backend-python/` 的最小可运行结构。
- 落地统一响应与错误码规范（PRODUCT.md §11.1/§11.2）中的会话相关部分。

## Capabilities

### New Capabilities

- `user-account`: 微信登录、平台会话与账户基础状态。后续协议确认、资料管理、注销等需求也归属此 capability。

### Modified Capabilities

（无）

## Impact

- 新增 `openapi/product-api.yaml`（`/session/wechat` 端点与响应结构）。
- 新增 `contracts/error-codes.yaml`（`SESSION_EXPIRED`、`INTERNAL_ERROR` 等会话相关错误码）。
- 新增 `backend-java/`（Spring Boot）与 `backend-python/`（FastAPI）两个等价实现骨架。
- 外部依赖：微信 `code2session` 接口（需 `appid`/`secret` 配置，不进入版本库）。
