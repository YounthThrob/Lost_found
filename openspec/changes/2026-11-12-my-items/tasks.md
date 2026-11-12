# Tasks

## 1. 契约落地

- [ ] 1.1 `openapi/product-api.yaml` 新增 `GET /me/posts` 与 `GET /me/claims`

## 2. Java 实现

- [ ] 2.1 `ItemPostStore` 增 `listByAuthor`；`ListingService` 增 `listMyPosts`
- [ ] 2.2 `ClaimStore` 增 `listByApplicant`；`ClaimService` 增 `listMyClaims`
- [ ] 2.3 `ListingController` 增 `GET /me/posts`；`ClaimController` 增 `GET /me/claims`
- [ ] 2.4 集成测试：隔离、分页、草稿可见

## 3. Python 实现

- [ ] 3.1 `listing` 增 `list_by_author`/`list_my_posts` 与路由
- [ ] 3.2 `claim` 增 `list_by_applicant`/`list_my_claims` 与路由
- [ ] 3.3 pytest：同 Java 覆盖项

## 4. 契约测试与等价性

- [ ] 4.1 扩展 `contracts/run_contract_tests.py`：`/me/posts` 与 `/me/claims` 探针
- [ ] 4.2 两后端分别运行契约测试验证一致

## Workflow follow-up

- 实现与契约测试通过后归档本 change，将 delta 合并进 `openspec/specs/my-items/`。
