# Tasks

## 1. 项目骨架

- [ ] 1.1 创建 `openapi/`、`contracts/`、`backend-java/`、`backend-python/` 目录，验证目录存在且符合 PRODUCT.md §12.1 结构
- [ ] 1.2 初始化 backend-java 最小可运行骨架（Spring Boot + Maven），验证启动命令能拉起服务
- [ ] 1.3 初始化 backend-python 最小可运行骨架（FastAPI + pyproject.toml），验证 uvicorn 能拉起服务

## 2. 契约落地

- [ ] 2.1 在 `openapi/product-api.yaml` 定义 `POST /api/v1/session/wechat` 与 `GET /api/v1/me`，验证 OpenAPI 文档校验通过
- [ ] 2.2 在 `contracts/error-codes.yaml` 记录 `SESSION_EXPIRED`、`WECHAT_CODE_INVALID`、`INTERNAL_ERROR`，验证错误码结构与 PRODUCT.md §11.2 一致

## 3. Java 实现

- [ ] 3.1 实现 code2session 客户端与 `POST /api/v1/session/wechat`，验证有效 code 返回 JWT、无效 code 返回 `WECHAT_CODE_INVALID`
- [ ] 3.2 实现 JWT 会话校验拦截器与 `GET /api/v1/me`，验证无/过期会话返回 `SESSION_EXPIRED`、有效会话返回 `id`/`status`
- [ ] 3.3 通过集成测试断言登录响应不含 `openid` 与 `session_key`

## 4. Python 实现

- [ ] 4.1 实现 code2session 调用与 `POST /api/v1/session/wechat`，验证有效 code 返回 JWT、无效 code 返回 `WECHAT_CODE_INVALID`
- [ ] 4.2 实现 JWT 会话校验依赖与 `GET /api/v1/me`，验证无/过期会话返回 `SESSION_EXPIRED`、有效会话返回 `id`/`status`
- [ ] 4.3 通过 pytest 断言登录响应不含 `openid` 与 `session_key`

## 5. 契约测试与等价性

- [ ] 5.1 编写共享黑盒契约测试（登录成功/无效 code/无会话访问/响应不含敏感字段），验证两后端对同一请求返回一致的状态码与响应结构
- [ ] 5.2 分别对 backend-java 与 backend-python 运行该契约测试，验证全部通过

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change（`/opsx:archive`），将 delta 合并进 `openspec/specs/user-account/`。
