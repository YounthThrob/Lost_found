# Tasks

## 1. 契约落地

- [x] 1.1 在 `openapi/product-api.yaml` 新增 `/posts`、`/posts/{postId}`、`/posts/{postId}/publish`、`/posts/{postId}/close` 及 ItemPost 相关 schema 与分页 schema
- [x] 1.2 在 `contracts/error-codes.yaml` 新增 `NOT_FOUND`（404）与 `INVALID_STATE_TRANSITION`（409），与 PRODUCT.md §11.2 一致

## 2. Java 实现

- [x] 2.1 新增 `ItemPost` record 与 `ItemPostStore` 内存存储（create/findById/listPublished/clear）
- [x] 2.2 新增 `ListingService`（createDraft/updateDraft/publish/close/getDetail/search，状态迁移与校验）
- [x] 2.3 新增 `ListingController` 六类端点，写接口标注 `@RequiresAgreement`
- [x] 2.4 新增 `NOT_FOUND`、`INVALID_STATE_TRANSITION` 错误码
- [x] 2.5 微信 mock 增加 `valid-code-2 → openid_test_002`
- [x] 2.6 集成测试：创建/编辑/发布/关闭/详情/搜索/非法迁移/未确认协议/校验失败/作者可见性

## 3. Python 实现

- [x] 3.1 新增 `ItemPost` dataclass 与 `InMemoryItemPostStore` 内存存储
- [x] 3.2 新增 `service`（create_draft/update_draft/publish/close/get_detail/search，状态迁移与校验）
- [x] 3.3 新增 `router` 六类端点，写接口依赖 `require_agreement`
- [x] 3.4 新增 `NOT_FOUND`、`INVALID_STATE_TRANSITION` 错误码
- [x] 3.5 微信 mock 增加 `valid-code-2 → openid_test_002`
- [x] 3.6 pytest：创建/编辑/发布/关闭/详情/搜索/非法迁移/未确认协议/校验失败/作者可见性

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：创建草稿、未确认协议 403、发布、搜索、详情、关闭、关闭后不可搜索、非法迁移 409、未找到 404、空标题 422、作者可见性
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致且符合契约

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change（`/opsx:archive`），将 delta 合并进 `openspec/specs/lost-found-listing/`。
