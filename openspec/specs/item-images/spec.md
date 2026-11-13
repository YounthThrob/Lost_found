# item-images Specification

## Purpose
为寻物/招领信息提供图片的上传、删除与可见性管理：图片二进制经存储抽象落地，数据库仅存存储键，公开信息图片公开、草稿图片仅作者可见。

## Requirements

### Requirement: 上传信息图片

作者 SHALL 能为自己的 `DRAFT` 信息上传图片。系统 SHALL 校验图片类型（jpeg/png/webp）、大小（≤10MB）与单帖数量（≤9 张），并把二进制交给存储抽象落地，数据库仅保存 `storage_key`。

#### Scenario: 上传成功

- **WHEN** 作者对 `DRAFT` 信息上传一张合法图片
- **THEN** 系统返回 200，data 包含图片 `id`、`url` 与 `sortOrder`，并计入该信息

#### Scenario: 图片类型不允许被拒绝

- **WHEN** 作者上传非 jpeg/png/webp 的图片
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 超过大小上限被拒绝

- **WHEN** 作者上传超过 10MB 的图片
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 超过数量上限被拒绝

- **WHEN** 信息已有 9 张图片且作者再次上传
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 非作者或非草稿被拒绝

- **WHEN** 非作者上传图片，或信息不是 `DRAFT` 状态
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404，非作者）或 `INVALID_STATE_TRANSITION`（HTTP 409，非草稿）且不保存

### Requirement: 删除信息图片

作者 SHALL 能删除自己 `DRAFT` 信息中的图片。系统 SHALL 从存储中删除图片二进制并移除对应元数据。

#### Scenario: 删除成功

- **WHEN** 作者删除自己草稿中的一张图片
- **THEN** 系统返回 200，该图片从信息中移除

#### Scenario: 非作者不可删除

- **WHEN** 非作者尝试删除他人信息中的图片
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）且不删除

#### Scenario: 非草稿不可删除

- **WHEN** 作者尝试删除非 `DRAFT` 信息中的图片
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不删除

### Requirement: 图片可见性

已发布信息的图片 SHALL 对所有人可见；草稿图片 SHALL 仅在作者请求时随信息返回，且 SHALL NOT 出现在公开搜索结果中。图片 URL SHALL 使用不可预测的存储键标识。

#### Scenario: 已发布图片公开

- **WHEN** 任意用户查看一条 `PUBLISHED` 或 `VERIFYING` 信息
- **THEN** 响应中的 `images` 包含可公开访问的图片 URL

#### Scenario: 草稿图片仅作者可见

- **WHEN** 非作者请求他人的 `DRAFT` 信息
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404），不泄露该草稿及图片
