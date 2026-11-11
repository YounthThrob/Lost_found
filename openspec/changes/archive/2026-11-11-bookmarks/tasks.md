# Tasks

## 1. 契约落地

- [x] 1.1 `openapi/product-api.yaml` 新增 3 个端点（POST/DELETE /posts/{postId}/bookmark、GET /me/bookmarks）
- [x] 1.2 `openapi/product-api.yaml` 新增 BookmarkView/BookmarkListData/BookmarkResponse/BookmarkListResponse/BookmarkDeleteResponse

## 2. Java 实现

- [x] 2.1 新增 `bookmark` 包：Bookmark 记录、BookmarkStore、BookmarkService、BookmarkController
- [x] 2.2 BookmarkService 接入公开可见性校验、幂等新增/删除、分页列表
- [x] 2.3 复用 ListingController.ItemPostView（from 改为 public）
- [x] 2.4 集成测试：收藏成功、重复幂等、非展示中拒绝、列表（含状态）、取消、取消幂等、隔离

## 3. Python 实现

- [x] 3.1 新增 `app/bookmark`（store、service、router）
- [x] 3.2 接入可见性校验、幂等新增/删除、分页列表
- [x] 3.3 pytest：同 Java 覆盖项

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：收藏/取消/幂等/可见性/隔离探针
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/bookmarks/`。
