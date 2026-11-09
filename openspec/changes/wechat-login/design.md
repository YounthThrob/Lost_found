# Design

## Context

- 当前仓库仅有 `PRODUCT.md` 与 OpenSpec 骨架，`openapi/`、`contracts/`、`backend-java/`、`backend-python/` 均未建立。
- 双后端等价约束（PRODUCT.md §12）：OpenAPI、错误码、状态机、测试向量是共同事实源；两实现不得共享运行时代码，但可共享契约与测试。
- 统一规范（PRODUCT.md §11）：统一响应格式 `data/requestId/timestamp`、UTC/RFC3339、错误码表。

## Goals / Non-Goals

**Goals:**

- 建立 walking skeleton：初始化四个目录，跑通登录端到端，用第一个契约测试验证双后端等价。
- 确定会话凭证方案，并让「受保护接口要求有效会话」可被验证。

**Non-Goals:**

- 不实现协议确认（C02）、资料管理（C03）、注销（C04）。
- 不实现订阅消息、图片上传、unionid 跨应用关联。

## Decisions

1. **会话凭证采用无状态 JWT（对称签名）**：登录成功后返回自签 JWT，密钥由环境变量注入。理由：无状态、Java（Spring Security）与 Python（PyJWT）都能等价实现、便于契约测试，满足「客户端无需修改即可在两后端间切换」。备选：服务端会话需引入共享存储，破坏两端独立部署，故不采用。

2. **`code2session` 由两后端各自实现 HTTP 调用**，`appid`/`secret` 从环境变量读取、不进仓库。登录响应只含 JWT，不下发 `openid` 与 `session_key`。

3. **仓库骨架按 PRODUCT.md §12.1**：`openapi/product-api.yaml`、`contracts/error-codes.yaml`、`backend-java/`、`backend-python/`。

4. **错误码**：在 `contracts/error-codes.yaml` 新增 `WECHAT_CODE_INVALID`（HTTP 401），复用 `SESSION_EXPIRED`、`INTERNAL_ERROR`。

5. **会话校验的验证落点**：本 change 实现 `POST /api/v1/session/wechat`，并最小化实现 `GET /api/v1/me`（只读返回当前用户的 `id`/`status`），作为「未登录被拒 / 有效会话放行」的验证点。`PATCH /api/v1/me` 的资料编辑留给 C03。

## Risks / Trade-offs

- [微信 code2session 依赖外部服务且需要 appid/secret] → 契约测试与本地开发使用 mock 或测试 appid；生产密钥由部署注入，不进仓库。
- [JWT 签名密钥管理] → 本 change 仅从环境变量注入；密钥轮换与 KMS 归 E3（私密数据加密）再细化。
- [双后端行为漂移] → 以同一 OpenAPI 与同一契约测试为事实源；本 change 建立首个契约测试用例。
- [openid 属于个人标识] → 仅服务端持有，登录响应与日志均不下发、不落库明文。

## Migration Plan

- 全新项目，无数据迁移。初始化骨架后，两端分别实现，并通过同一套契约测试。
