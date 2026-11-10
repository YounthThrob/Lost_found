# Spec Delta

## Purpose

定义认领申请从提交、查看、请求补充到接受/拒绝的完整生命周期，以及申请与招领信息状态的服务端联动，确保申请内容仅对申请人与发布者可见、状态迁移由服务端校验。

## ADDED Requirements

### Requirement: 提交认领申请

已确认协议的用户 SHALL 能够对展示中的招领（FOUND）信息提交认领申请，申请 SHALL 包含非空的归属证明 `statement`。系统 SHALL 校验信息可认领（FOUND、PUBLISHED 或 VERIFYING、非作者本人），同一用户对同一信息 SHALL 仅存在一个进行中的申请，首个申请到达时信息迁移至 `VERIFYING`。

#### Scenario: 提交成功

- **WHEN** 已确认协议的用户对展示中的招领信息提交非空 `statement`
- **THEN** 系统保存申请，返回 `status` 为 `SUBMITTED`，且信息状态迁移为 `VERIFYING`

#### Scenario: 未确认协议被拒绝

- **WHEN** 未确认协议的用户尝试提交申请
- **THEN** 系统返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不保存

#### Scenario: 信息不可认领被拒绝

- **WHEN** 用户对寻物（LOST）、非公开状态或本人发布的信息提交申请
- **THEN** 系统返回 `POST_NOT_CLAIMABLE`（HTTP 409）且不保存

#### Scenario: 重复申请被拒绝

- **WHEN** 用户对同一信息在已有进行中申请的情况下再次提交
- **THEN** 系统返回 `CLAIM_DUPLICATE`（HTTP 409）且不保存

#### Scenario: 归属证明为空被拒绝

- **WHEN** 用户提交空或超长的 `statement`
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 发布者查看收到的申请

信息发布者 SHALL 能够查看自己信息收到的认领申请列表，结果分页返回并按提交时间倒序；非发布者 SHALL NOT 查看。

#### Scenario: 发布者查看成功

- **WHEN** 信息发布者查询该信息收到的申请
- **THEN** 系统返回申请列表与分页信息

#### Scenario: 非发布者不可查看

- **WHEN** 非发布者尝试查看他人信息收到的申请
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 请求补充信息

信息发布者 SHALL 能够对 `SUBMITTED` 状态的申请请求补充信息，申请迁移至 `NEEDS_MORE_INFO`。系统 SHALL 仅允许发布者操作，且仅允许从 `SUBMITTED` 迁移。

#### Scenario: 请求补充成功

- **WHEN** 发布者对 `SUBMITTED` 申请请求补充
- **THEN** 系统将申请状态迁移为 `NEEDS_MORE_INFO` 并返回更新后的申请

#### Scenario: 非法迁移被拒绝

- **WHEN** 发布者对非 `SUBMITTED` 状态的申请请求补充
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

#### Scenario: 非发布者被拒绝

- **WHEN** 非发布者尝试请求补充
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 接受申请

信息发布者 SHALL 能够接受 `SUBMITTED` 或 `NEEDS_MORE_INFO` 状态的申请，申请迁移至 `ACCEPTED`，信息迁移至 `PENDING_HANDOVER`。同一信息同一时间 SHALL 至多有一个申请被接受。

#### Scenario: 接受成功

- **WHEN** 发布者接受一个 `SUBMITTED` 或 `NEEDS_MORE_INFO` 的申请
- **THEN** 系统将申请迁移为 `ACCEPTED`，信息迁移为 `PENDING_HANDOVER`

#### Scenario: 已有申请进入待交接被拒绝

- **WHEN** 发布者尝试接受另一申请而该信息已有 `ACCEPTED` 申请
- **THEN** 系统返回 `HANDOVER_CONFLICT`（HTTP 409）且不改变状态

#### Scenario: 非法迁移被拒绝

- **WHEN** 发布者接受非 `SUBMITTED`/`NEEDS_MORE_INFO` 状态的申请
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

### Requirement: 拒绝申请

信息发布者 SHALL 能够拒绝 `SUBMITTED` 或 `NEEDS_MORE_INFO` 状态的申请，申请迁移至 `REJECTED`。若信息处于 `VERIFYING` 且不再有进行中的申请，信息 SHALL 回到 `PUBLISHED`。

#### Scenario: 拒绝成功

- **WHEN** 发布者拒绝一个 `SUBMITTED` 或 `NEEDS_MORE_INFO` 的申请
- **THEN** 系统将申请迁移为 `REJECTED`

#### Scenario: 全部申请关闭后信息回到已发布

- **WHEN** `VERIFYING` 信息的最后一个进行中申请被拒绝
- **THEN** 信息状态迁移回 `PUBLISHED`

#### Scenario: 非法迁移被拒绝

- **WHEN** 发布者拒绝非 `SUBMITTED`/`NEEDS_MORE_INFO` 状态的申请
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

### Requirement: 申请内容隐私保护

申请 `statement` 与申请人标识 SHALL 仅对申请人与信息发布者可见，SHALL NOT 出现在公开搜索、信息详情、普通日志或任何公开接口中。

#### Scenario: 公开接口不泄露申请内容

- **WHEN** 任意用户查询公开信息列表或详情
- **THEN** 响应 SHALL NOT 包含任何申请的 `statement` 或申请人标识
