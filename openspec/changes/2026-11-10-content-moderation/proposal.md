# Proposal

## Why

C04 已落地寻物/招领的发布与生命周期，但发布流程目前无条件通过审核，且平台尚无举报入口——「内容检测」与「举报」是守住非官方、反诈骗、反骚扰边界并形成运营闭环的关键。本 change 落地 `content-moderation` capability（PRODUCT.md §17 顺序 4）的 MVP 切片：发布时对公开内容做确定性违禁词检测（命中则 REJECTED），并支持用户举报信息/用户、查看自己的举报。运营管理动作（下架/禁言/封禁，需运营角色鉴权）另立 change。

## What Changes

- 发布流程接入自动内容检测：`DRAFT → PENDING_REVIEW` 后，命中违禁关键词迁移至 `REJECTED`，否则 `PUBLISHED`（状态机新增 `REJECTED`）。
- 新增 `POST /api/v1/reports` 提交举报（信息或用户）；`GET /api/v1/reports/me` 分页查看自己的举报。
- 新增 `Report` 内存存储与 `ContentModerator` 确定性检测器；违禁词表落 `contracts/moderation-rules.yaml` 作为共同事实源。
- 同步更新 OpenAPI 契约，Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `content-moderation`: 发布时内容自动检测、举报提交与举报查询。

### Modified Capabilities

- `lost-found-listing`: 发布状态机新增 `REJECTED`，发布流程接入自动检测。

## Impact

- 新增 `contracts/moderation-rules.yaml`（违禁词表）。
- 修改 `openapi/product-api.yaml`：`/publish` 状态枚举加入 `REJECTED`、新增 `/reports`、`/reports/me` 端点与 Report schema。
- 修改 `backend-java/` 与 `backend-python/`：listing 发布接入检测器；新增 moderation 模块（Report、检测器、控制器）与测试。
- 扩展 `contracts/run_contract_tests.py`：违禁发布被拒、被拒信息不可搜索、举报提交/非法类型/我的举报。
