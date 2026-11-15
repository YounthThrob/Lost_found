# Proposal

## Why

运营后台目前复用微信 mock 登录（`operator-code → openid_operator_001`），只能在 `WECHAT_MOCK=true` 的本地环境工作，且相当于保留了一条 mock 后门。运营需要一个正式、独立于微信小程序的登录机制：账号密码登录，以建立运营会话并驱动运营管理动作。

## What Changes

- 新增运营账号密码登录端点 `POST /session/operator`（`username` + `password`），校验通过后返回会话凭证（JWT）。
- 新增业务错误码 `INVALID_CREDENTIALS`（HTTP 401），用户名或密码错误时返回，不建立会话。
- 后端内置单个运营账号（用户名 `admin`，密码以 bcrypt 哈希存储、可经环境变量覆盖，角色为 `OPERATOR`）。
- **BREAKING**：移除 `operator-code → openid_operator_001` 的微信 mock 运营登录映射；运营端点认证改为账号密码，运营不再走微信登录。
- 运营管理 Web 端登录页改为用户名/密码表单，删除 mock code 预填。
- 契约测试与双后端单元测试改用账号密码登录运营端点。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `operations-management`：新增「运营通过账号密码登录」需求；运营身份由账号密码会话建立，不再依赖微信 mock openid。

## Impact

- 契约：`openapi/product-api.yaml` 新增 `/session/operator`；`contracts/error-codes.yaml` 新增 `INVALID_CREDENTIALS`；`contracts/run_contract_tests.py` 运营登录改用账号密码。
- 后端：Java 与 Python 新增运营凭据存储与登录校验（bcrypt），移除 `operator-code` mock 映射。
- 前端：`admin-web` 登录页改账号密码表单，`config.ts` 移除 `OPERATOR_CODE`。
- 测试：Java `OperationsTest`、Python `test_operations` 及契约测试的运营认证向量更新。
