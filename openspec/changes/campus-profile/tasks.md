# Tasks

## 1. 契约落地

- [x] 1.1 在 `openapi/product-api.yaml` 新增 `GET/PUT /me/campus-profile`、`CampusProfileUpdate` 与 `CampusProfileResponse`，验证 OpenAPI 校验通过
- [x] 1.2 在 `contracts/error-codes.yaml` 新增 `VALIDATION_FAILED`（422），与 PRODUCT.md §11.2 一致

## 2. Java 实现

- [x] 2.1 `UserAccount` 增加 `school`/`campus` 字段；`InMemoryUserStore` 新增 `updateCampus`（保留协议状态）
- [x] 2.2 `AccountService` 增加 campus-profile 查询与更新（校验学校非空、长度合规）
- [x] 2.3 `AccountController` 新增 `GET/PUT /me/campus-profile`，响应含 `verificationStatus: SELF_DECLARED`
- [x] 2.4 新增 `VALIDATION_FAILED` 错误码与 `HttpMessageNotReadableException` 处理器
- [x] 2.5 集成测试：未设置返回 null、设置后返回、空学校返回 422、响应无认证类字段

## 3. Python 实现

- [x] 3.1 `UserAccount` 增加 `school`/`campus` 字段；store 新增 `update_campus`（保留协议状态）
- [x] 3.2 `service` 增加 campus-profile 查询与更新（校验学校非空、长度合规）
- [x] 3.3 `router` 新增 `GET/PUT /me/campus-profile`，响应含 `verificationStatus: SELF_DECLARED`
- [x] 3.4 `RequestValidationError` 处理器改为统一 `VALIDATION_FAILED` 信封
- [x] 3.5 pytest：未设置返回 null、设置后返回、空学校返回 422、响应无认证类字段

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：GET 空、PUT、GET 已设、空学校 422、verificationStatus 恒 SELF_DECLARED
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致且符合契约

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change（`/opsx:archive`），将 delta 合并进 `openspec/specs/self-declared-campus/`。
