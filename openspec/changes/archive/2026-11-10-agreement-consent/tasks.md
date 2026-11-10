# Tasks

## 1. 契约落地

- [x] 1.1 在 `openapi/product-api.yaml` 为 `LoginResponse`、`MeResponse` 增加 `agreementAccepted`（boolean），新增 `POST /me/agreement` 端点与响应结构，验证 OpenAPI 校验通过
- [x] 1.2 在 `contracts/error-codes.yaml` 新增 `AGREEMENT_NOT_ACCEPTED`（403），与 PRODUCT.md §11.2 一致

## 2. Java 实现

- [x] 2.1 `UserAccount` 增加 `agreementAcceptedAt`（可空 RFC3339），`InMemoryUserStore` 支持读取与写入；新账户默认未确认
- [x] 2.2 登录响应与 `GET /me` 返回 `agreementAccepted`；新增 `POST /me/agreement`（幂等，记录确认时间）
- [x] 2.3 实现 `@RequiresAgreement` 注解 + `HandlerInterceptor` + `AgreementGuard`，未确认抛 `AGREEMENT_NOT_ACCEPTED`
- [x] 2.4 通过集成测试断言：新用户 `agreementAccepted=false`、确认后 `/me` 为 `true`、门控组件对未确认用户抛 `AGREEMENT_NOT_ACCEPTED`

## 3. Python 实现

- [x] 3.1 账户模型增加 `agreement_accepted_at`（可空），store 支持读写；新账户默认未确认
- [x] 3.2 登录响应与 `GET /me` 返回 `agreementAccepted`；新增 `POST /me/agreement`（幂等，记录确认时间）
- [x] 3.3 实现 `require_agreement` 依赖，未确认抛 `AGREEMENT_NOT_ACCEPTED`
- [x] 3.4 通过 pytest 断言：新用户 `agreementAccepted=false`、确认后 `/me` 为 `true`、门控依赖对未确认用户抛 `AGREEMENT_NOT_ACCEPTED`

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：断言登录返回 `agreementAccepted=false`、确认接口后 `/me` 返回 `agreementAccepted=true`
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致且符合契约

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change（`/opsx:archive`），将 delta 合并进 `openspec/specs/user-account/`。
