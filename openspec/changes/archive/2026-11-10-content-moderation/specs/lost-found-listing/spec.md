# Spec Delta

## Purpose

修正发布状态机的自动审核表述：发布提交后由自动内容检测决定迁移至 PUBLISHED（通过）或 REJECTED（命中违禁词），并保留客户端不得任意指定目标状态的约束。

## MODIFIED Requirements

### Requirement: 发布信息并校验状态迁移

作者 SHALL 能够提交草稿发布。系统 SHALL 在服务端校验状态迁移：仅 `DRAFT` 可迁移至 `PENDING_REVIEW`，随后经自动内容检测迁移至 `PUBLISHED` 或 `REJECTED`。客户端 SHALL NOT 任意指定目标状态。

#### Scenario: 发布成功

- **WHEN** 作者对 `DRAFT` 信息提交发布且内容未命中违禁词
- **THEN** 系统将状态迁移为 `PUBLISHED` 并返回更新后的信息

#### Scenario: 命中违禁词被拒绝

- **WHEN** 作者对 `DRAFT` 信息提交发布且内容命中违禁词
- **THEN** 系统将状态迁移为 `REJECTED` 并返回更新后的信息

#### Scenario: 重复发布被拒绝

- **WHEN** 作者对非 `DRAFT` 状态（如已发布）的信息再次提交发布
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态
