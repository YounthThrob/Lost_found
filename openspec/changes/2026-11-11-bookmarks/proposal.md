# Proposal

## Why

后端 MVP 已覆盖账户、发布、认领、沟通、交接、举报、线索与限流，但 §8 的 REQ-010「收藏」尚未落地：用户无法收藏/取消收藏展示中的信息，也无法在「我的收藏」中查看已收藏信息。本 change 落地 `bookmarks` capability（PRODUCT.md REQ-010）的 MVP 切片：收藏/取消收藏展示中的信息、查询我的收藏，并落实「收藏仅自己可见、收藏量不对外展示、被收藏信息状态变化后仍保留但显示状态提示」的隐私边界。

## What Changes

- 新增 3 个契约端点：`POST /posts/{postId}/bookmark`（收藏，幂等）、`DELETE /posts/{postId}/bookmark`（取消收藏，幂等）、`GET /me/bookmarks`（查询我的收藏，分页）。
- 新增 `Bookmark` 关系模型（user_id + post_id 联合唯一，无状态机，见 PRODUCT.md §9.3/§10）。
- 收藏仅对「展示中」（`PUBLISHED`/`VERIFYING`）信息开放；对不存在或非展示中的信息返回 `NOT_FOUND`（404）。
- 收藏列表返回每条收藏的信息当前状态（`post.status`），供客户端标注「已下架/已关闭」并阻止进入详情。
- 收藏量不对外展示、不进入账户可信信息、不进入任何公开接口。
- Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `bookmarks`：收藏/取消收藏展示中的信息、查询我的收藏、收藏可见性与数量边界。

### Modified Capabilities

（无）

## Impact

- 修改 `openapi/product-api.yaml`：新增 3 个端点与 `BookmarkView`、`BookmarkListData`、`BookmarkResponse`、`BookmarkListResponse`、`BookmarkDeleteResponse` schema。
- 修改 `backend-java/` 与 `backend-python/`：新增 bookmark 存储、service、controller/router；复用公开可见性判定。
- 扩展 `contracts/run_contract_tests.py`：收藏成功/幂等/非展示中拒绝/收藏列表/取消收藏/收藏隔离等探针。
- 无新增错误码、无新增限流配置。
