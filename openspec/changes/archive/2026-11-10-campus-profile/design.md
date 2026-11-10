# Design

## Context

- 已有 `user-account`（微信登录 + 协议确认）。账户为 in-memory 存储，`UserAccount` 为不可变 record/dataclass。
- REQ-001：用户可修改自报学校与校区；修改后必须显示「用户自报，未经校方认证」。
- PRODUCT.md §11 API：`GET/PUT /api/v1/me/campus-profile` 管理自报学校信息。
- 非官方边界（CLAUDE.md 约束 4）：不得暗示「校方认证/身份可信/官方认证」。

## Goals / Non-Goals

**Goals:**

- 提供 campus-profile 的设置与查询接口，学校必填、校区可选。
- 在响应中恒返回 `verificationStatus: "SELF_DECLARED"`，从契约层杜绝「认证」暗示。
- 落地 `VALIDATION_FAILED`（422）校验错误，并让两后端的校验响应一致。

**Non-Goals:**

- 不实现昵称/头像编辑（头像涉及图片上传，另立 change）。
- 不实现学校/校区下拉数据源或校区运营配置（后续 change）。
- 不把 campus-profile 接入发布/认领（C06 等再接入）；本 change 仅落地数据与接口。
- 不将 campus-profile 门控于协议确认（发布/留言/认领才是受限操作）。

## Decisions

1. **数据模型**：campus-profile 作为账户的两个可空字段（`school`、`campus`），in-memory 存储；`verificationStatus` 不落库，是派生常量 `SELF_DECLARED`。理由：避免单独实体与持久化复杂度，标记本就无需存储。

2. **非官方标记**：响应仅含 `verificationStatus: "SELF_DECLARED"`，不含任何 `verified`/`certified` 字段；客户端据此渲染「用户自报，未经校方认证」。契约测试断言该字段恒为 `SELF_DECLARED` 且无认证类字段。

3. **端点**：`GET` 与 `PUT /api/v1/me/campus-profile`，不改 `GET /me`。GET 未设置时返回 `school`/`campus` 为 `null`。

4. **校验**：`school` 必填、trim 后非空、≤100 字符；`campus` 可选、≤100 字符。违规返回 `VALIDATION_FAILED`（422）。

5. **校验响应一致性**：
   - Python：将 FastAPI 的 `RequestValidationError` 处理器改为统一 `VALIDATION_FAILED` 信封；业务校验在 service 抛 `BusinessError(VALIDATION_FAILED)`。
   - Java：新增 `HttpMessageNotReadableException` → `VALIDATION_FAILED` 处理器；业务校验在 service 抛 `BusinessException(VALIDATION_FAILED)`。
   - 两端对同一非法输入返回一致的 422 与错误码。

## Risks / Trade-offs

- [校验一致性涉及跨后端错误处理] → 以 `VALIDATION_FAILED` 为统一错误码，契约测试覆盖「空学校 → 422」场景。
- [不可变 record 增加字段后需在所有重建点保留既有字段] → `confirmAgreement` 与 `updateCampus` 均重建完整对象，避免字段被意外清空；用单测覆盖「先确认协议再设校区」与「先设校区再确认协议」顺序。
- [「用户自报」文案归属] → 文案渲染归小程序端；后端只提供 `SELF_DECLARED` 机器标记，避免双后端维护文案。

## Migration Plan

- 无既有数据迁移（in-memory）。默认 `school`/`campus` 为 `null`。
