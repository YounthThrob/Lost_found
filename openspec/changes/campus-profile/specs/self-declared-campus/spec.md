# Spec Delta

## Purpose

让用户设置并维护自报学校与校区，并在所有展示场景中始终标记为「用户自报」，绝不暗示校园身份认证。

## ADDED Requirements

### Requirement: 用户设置自报学校与校区

用户 SHALL 能够通过更新接口设置或修改自报学校（必填）与校区（可选），系统 SHALL 校验学校非空且长度合规。

#### Scenario: 首次设置学校与校区

- **WHEN** 用户提交学校与校区
- **THEN** 系统保存并返回自报学校与校区

#### Scenario: 修改自报学校

- **WHEN** 用户提交新的学校或校区
- **THEN** 系统覆盖旧值并返回更新后的信息

#### Scenario: 学校为空被拒绝

- **WHEN** 用户提交空学校
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 查询自报学校与校区

用户 SHALL 能够查询自己的自报学校与校区。

#### Scenario: 未设置时返回空

- **WHEN** 用户尚未设置学校
- **THEN** 系统返回学校与校区均为 `null`

#### Scenario: 已设置时返回保存值

- **WHEN** 用户已设置学校
- **THEN** 系统返回已保存的学校与校区

### Requirement: 自报学校标记为非认证

系统 SHALL 将自报学校始终标记为 `verificationStatus: "SELF_DECLARED"`，且 SHALL NOT 返回任何表示「已认证」「可信」或「官方」的状态或字段。

#### Scenario: 响应携带非认证标记

- **WHEN** 用户查询自报学校
- **THEN** 响应中 `verificationStatus` 恒为 `SELF_DECLARED`

#### Scenario: 响应不暗示认证

- **WHEN** 用户查询自报学校
- **THEN** 响应 SHALL NOT 包含 `verified`、`certified` 或其它认证类字段
