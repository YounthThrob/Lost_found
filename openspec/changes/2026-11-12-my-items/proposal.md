# 我的发布与我的申请

## Why

客户端「我的」模块需要展示「我的发布」与「我的申请」两个列表（PRODUCT.md §6 页面清单）。当前 API 仅有公开搜索 `GET /posts` 与发布者按信息查看申请的 `GET /posts/{postId}/claims`，无法让用户查看自己全部信息（含草稿/已关闭）与自己作为申请人提交的全部申请。为支撑客户端完整 MVP 界面，补充两个自作用域列表端点。

## What Changes

- 新增 `GET /api/v1/me/posts`：返回当前用户全部信息（不分状态），分页，按创建时间倒序。
- 新增 `GET /api/v1/me/claims`：返回当前用户作为申请人的全部申请，分页，按创建时间倒序。

两接口复用现有 `ItemPostView`/`ClaimView` 与分页结构，不新增错误码、状态或字段。

## Impact

- 契约：`openapi/product-api.yaml` 新增两个路径。
- Java：`ListingService`/`ClaimService` 增列表方法；`ItemPostStore`/`ClaimStore` 增按作者/申请人查询。
- Python：`listing`/`claim` 模块对应补齐。
- 契约测试：新增两个探针验证隔离与分页。
