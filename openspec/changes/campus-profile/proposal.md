# Proposal

## Why

用户完成登录与协议确认后，需要维护「自报学校与校区」这一核心身份信息——它是后续发布、搜索、认领场景中区分不同校园与校区、并守住「非官方」边界的关键。REQ-001 要求用户可修改自报学校与校区，且任何展示都必须明确标注「用户自报，未经校方认证」，绝不暗示校园身份认证（CLAUDE.md 约束 4）。本 change 落地独立的 `self-declared-campus` capability（PRODUCT.md §17 顺序 2），兑现该非官方边界。

## What Changes

- 新增 `GET/PUT /api/v1/me/campus-profile`：查询/设置当前用户的自报学校（必填）与校区（可选）。
- 账户新增自报学校、校区两个可空字段（in-memory 存储）。
- 所有查询/更新响应恒返回 `verificationStatus: "SELF_DECLARED"`，且不返回任何 `verified`/`certified` 等认证类字段。
- 新增错误码 `VALIDATION_FAILED`（HTTP 422）：学校为空或超长等参数校验失败时返回。
- 同步更新 OpenAPI 契约与错误码表，Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `self-declared-campus`: 用户自报学校与校区的设置、查询，以及「非认证」标记。

### Modified Capabilities

（无）

## Impact

- 修改 `openapi/product-api.yaml`：新增 `/me/campus-profile` 端点、`CampusProfileUpdate` 与 `CampusProfileResponse` 结构。
- 修改 `contracts/error-codes.yaml`：新增 `VALIDATION_FAILED`（422）。
- 修改 `backend-java/` 与 `backend-python/`：账户模型、campus-profile 端点、校验与测试。
