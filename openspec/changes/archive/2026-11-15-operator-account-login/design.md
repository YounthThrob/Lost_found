# Design

## Context

当前运营身份通过微信 mock 登录建立：`WechatClient`/`wechat_client` 把 `operator-code` 映射为 `openid_operator_001`，`AccountService.login` 据此把角色置为 `OPERATOR`。JWT 只携带 `subject = userId`（不含角色），角色在服务端经 `OperatorGuard`/`require_operator` 从用户记录读取。本次改为独立的账号密码登录，见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 提供运营专属的账号密码登录，独立于微信小程序登录。
- 密码仅以单向哈希存储，任何接口不返回密码或哈希。
- 双后端行为一致（同 HTTP 状态码、错误码、字段）。

**Non-Goals:**

- 多运营账号管理、运营账号增删改/禁用、密码找回、双因素认证。
- 把角色写入 JWT（继续由服务端从用户记录读取）。

## Decisions

1. **登录端点**：新增 `POST /session/operator`，body `{username, password}`，响应 `{token}`。不复用 `/session/wechat`：语义与错误码不同（微信用 `WECHAT_CODE_INVALID`，运营用 `INVALID_CREDENTIALS`）。

2. **凭据模型**：`UserAccount` 增加可空的 `username`/`passwordHash`（仅运营账户使用）。启动时种子单个运营账户：`id` 固定、`role=OPERATOR`、`username` 取环境变量 `OPERATOR_USERNAME`（默认 `admin`）、`passwordHash = bcrypt(OPERATOR_PASSWORD)`（默认 `admin123`）。运营登录按 `username` 查找、bcrypt 校验、签发 JWT（subject=userId）。

3. **密码哈希**：bcrypt。Java 用 Spring Security `BCryptPasswordEncoder`（`spring-security-crypto` 已有传递依赖）；Python 新增 `bcrypt` 依赖（`hashpw`/`checkpw`，`$2b$` 与 `$2a$` 兼容）。各后端种子各自的哈希，跨后端不共享；契约测试只校验行为，不校验哈希格式。

4. **移除 mock 运营登录**：删除 `WechatClient`/`wechat_client` 中 `operator-code → openid_operator_001` 映射与 `OPERATOR_OPENID` 角色判定；所有微信登录均建立 `USER` 角色。运营角色仅由种子运营账户 + 账号密码登录建立。

5. **错误码**：新增 `INVALID_CREDENTIALS`（HTTP 401，消息「用户名或密码错误」），加入 `error-codes.yaml` 与双后端 `ErrorCode`。

6. **测试**：契约测试运营登录改为 `POST /session/operator`（`admin`/`admin123`），并新增错误密码向量；Java `OperationsTest`、Python `test_operations` 的 `operator-code` 登录改为账号密码。

## Risks / Trade-offs

- [开发默认密码 `admin123` 可被猜到] → 仅本地开发默认；生产通过 `OPERATOR_PASSWORD` 覆盖，并在部署文档注明必须修改。
- [移除 `operator-code` 影响既有测试/联调] → 契约测试与单测同步更新，双后端一起改，避免半边不一致。
- [用户名枚举] → 用户名不存在与密码错误统一返回 `INVALID_CREDENTIALS`，不区分两者。

## Migration Plan

1. 契约优先：改 `openapi/product-api.yaml`、`contracts/error-codes.yaml`、`contracts/run_contract_tests.py`。
2. 双后端实现登录端点、种子运营账户、移除 mock 映射。
3. `admin-web` 登录页改账号密码表单，`config.ts` 移除 `OPERATOR_CODE`。
4. 更新单测与契约测试，跑绿后归档。

## Open Questions

（无）
