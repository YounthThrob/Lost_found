# Proposal

## Why

用户登录、确认协议并设置自报学校后，平台的核心价值——「发布寻物/招领 → 搜索发现 → 生命周期闭环」——尚无承载。这是失物招领产品的主干：失主与拾获者需要一个结构化的信息对象，并能在其上安全地发布、检索和关闭。本 change 落地 `lost-found-listing` capability（PRODUCT.md §17 顺序 3），建立 ItemPost 数据模型、服务端校验的状态机（草稿 → 待审核 → 已发布 → 已关闭）以及公开搜索的分页契约，为后续认领、沟通、交接能力打下数据基础。

## What Changes

- 新增 `POST /api/v1/posts` 创建寻物/招领草稿；`PATCH /api/v1/posts/{postId}` 编辑草稿。
- 新增 `POST /api/v1/posts/{postId}/publish` 提交发布（DRAFT → PENDING_REVIEW → PUBLISHED，MVP 自动通过审核）；`POST /api/v1/posts/{postId}/close` 主动关闭（PUBLISHED → CLOSED）。
- 新增 `GET /api/v1/posts` 搜索/筛选（仅返回已发布），落地分页格式 `{items, pagination}`；`GET /api/v1/posts/{postId}` 查看详情（已发布对所有人可见，非已发布仅作者可见）。
- 新增 `ItemPost` 内存存储；发布/编辑/关闭接口受协议门控（`AGREEMENT_NOT_ACCEPTED`）。
- 新增错误码 `NOT_FOUND`（404）与 `INVALID_STATE_TRANSITION`（409），状态迁移由服务端校验。
- 同步更新 OpenAPI 契约与错误码表，Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `lost-found-listing`: 寻物/招领信息的创建、编辑、发布、关闭、详情与搜索，以及服务端校验的生命周期状态机。

### Modified Capabilities

（无）

## Impact

- 修改 `openapi/product-api.yaml`：新增 `/posts`、`/posts/{postId}`、`/posts/{postId}/publish`、`/posts/{postId}/close` 端点及 ItemPost 相关 schema 与分页 schema。
- 修改 `contracts/error-codes.yaml`：新增 `NOT_FOUND`（404）、`INVALID_STATE_TRANSITION`（409）。
- 修改 `backend-java/`：新增 `listing` 包（ItemPost、store、service、controller）与测试；微信 mock 增加第二个确定性 code。
- 修改 `backend-python/`：新增 `app/listing` 模块与测试；微信 mock 增加第二个确定性 code。
- 扩展 `contracts/run_contract_tests.py`：覆盖创建、发布、搜索、详情、关闭、非法迁移、未找到、校验失败与作者可见性等场景。
