# Tasks

## 1. 契约（契约优先）

- [x] 1.1 更新 `openapi/product-api.yaml`：新增 `POST /session/operator` 端点（body `username`/`password`）与 `OperatorLoginResponse` schema；`/session/wechat` 不受影响。验证：全文无 `operator-code` 残留，端点可被解析。
- [x] 1.2 在 `contracts/error-codes.yaml` 新增 `INVALID_CREDENTIALS`（HTTP 401）。验证：与双后端 `ErrorCode` 定义一致。
- [x] 1.3 更新 `contracts/run_contract_tests.py`：运营登录改用 `POST /session/operator`（`admin`/`admin123`），并新增错误密码返回 401 `INVALID_CREDENTIALS` 向量。验证：新增向量与 spec 各 Scenario 一一对应。

## 2. Python 后端

- [x] 2.1 新增 `bcrypt` 依赖；`UserAccount` 增加 `username`/`passwordHash`；种子单个运营账户（固定 id、`role=OPERATOR`、默认 `admin`/`admin123`、环境变量可覆盖）；移除 `wechat_client` 的 `operator-code` 映射与 `OPERATOR_OPENID` 角色判定。验证：`backend-python/.venv/Scripts/python.exe -m pytest` 相关单测通过。
- [x] 2.2 account 服务与路由新增 `POST /session/operator`：按 `username` 查找、bcrypt 校验、签发 JWT；用户名或密码错误返回 `INVALID_CREDENTIALS`（401）；响应不泄露密码/哈希。验证：curl 正确与错误密码行为符合 spec。
- [x] 2.3 更新 `test_operations.py` 等既有测试的 `operator-code` 登录为账号密码；新增 `test_operator_login.py`。验证：`pytest` 全绿。

## 3. Java 后端（镜像 Python）

- [x] 3.1 `UserAccount` 增加 `username`/`passwordHash`；种子运营账户；移除 `WechatClient` 的 `operator-code` 映射与 `OPERATOR_OPENID` 判定。验证：`JAVA_HOME=/d/develop/Java/jdk-21 mvn -q test` 编译通过。
- [x] 3.2 `AccountController`/`AccountService` 新增 `POST /session/operator`（`BCryptPasswordEncoder` 校验）。验证：行为与 Python 一致。
- [x] 3.3 更新 `OperationsTest` 的 `operator-code` 登录为账号密码；新增 `OperatorLoginTest`。验证：`mvn test` 全绿。

## 4. 双后端契约等价

- [x] 4.1 构建 Java 并运行 `contracts/run_contract_tests.py`，确认 Java == Python == expected 全部通过。验证：契约测试全绿、无失败向量。

## 5. 运营 Web 端

- [x] 5.1 `admin-web` 登录页改为账号密码表单，`config.ts` 移除 `OPERATOR_CODE`，登录请求改走 `/session/operator`。验证：`node node_modules/typescript/bin/tsc --noEmit` 无错误，可用 `admin`/`admin123` 登录。
- [x] 5.2 更新 `admin-web/README.md` 的登录说明为账号密码。验证：文档与实际行为一致。

## Workflow follow-up

- 完成后按项目规范 `/opsx:archive` 归档本 change。
