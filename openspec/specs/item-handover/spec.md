# item-handover Specification

## Purpose
定义已接受认领申请的交接安排创建、双方分别确认完成与任一方终止的完整生命周期，以及交接与信息/申请状态的服务端联动，确保交接内容仅对交接双方可见、状态迁移由服务端校验。

## Requirements

### Requirement: 创建交接安排

信息发布者或申请人 SHALL 能够为已接受（`ACCEPTED`）的认领申请创建交接安排，包含时间窗口与公共区域。系统 SHALL 校验操作者为交接参与者、申请已接受，且同一申请至多存在一个交接安排。

#### Scenario: 创建成功

- **WHEN** 发布者或申请人为 `ACCEPTED` 申请提交非空的时间窗口与公共区域
- **THEN** 系统保存交接安排并返回 `status` 为 `PENDING`

#### Scenario: 非参与者被拒绝

- **WHEN** 既非发布者也非申请人的用户尝试创建交接安排
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）且不保存

#### Scenario: 申请未接受被拒绝

- **WHEN** 参与者为非 `ACCEPTED` 状态的申请创建交接安排
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不保存

#### Scenario: 重复创建被拒绝

- **WHEN** 参与者为已有交接安排的申请再次创建交接安排
- **THEN** 系统返回 `HANDOVER_ALREADY_EXISTS`（HTTP 409）且不保存

#### Scenario: 字段缺失被拒绝

- **WHEN** 参与者提交空或超长的时间窗口或公共区域
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 双方确认完成

交接双方（信息发布者与申请人）SHALL 分别确认交接，系统 SHALL 记录各方的确认时间；仅当双方均确认后，交接 SHALL 迁移至 `COMPLETED`，申请 SHALL 迁移至 `COMPLETED`，信息 SHALL 迁移至 `COMPLETED`。

#### Scenario: 单方确认仍待交接

- **WHEN** 一方确认 `PENDING` 交接
- **THEN** 系统记录该方确认时间，交接保持 `PENDING`，申请与信息状态不变

#### Scenario: 双方确认完成

- **WHEN** 双方均已确认 `PENDING` 交接
- **THEN** 交接迁移为 `COMPLETED`，申请迁移为 `COMPLETED`，信息迁移为 `COMPLETED`

#### Scenario: 非法状态被拒绝

- **WHEN** 参与者对非 `PENDING` 交接确认
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

#### Scenario: 非参与者被拒绝

- **WHEN** 非参与者尝试确认交接
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 终止交接

交接任一方 SHALL 能够终止 `PENDING` 状态的交接，交接 SHALL 迁移至 `CANCELLED`，申请 SHALL 迁移至 `CANCELLED`，信息 SHALL 迁移回 `PUBLISHED`。

#### Scenario: 终止成功

- **WHEN** 任一方终止 `PENDING` 交接
- **THEN** 交接迁移为 `CANCELLED`，申请迁移为 `CANCELLED`，信息迁移为 `PUBLISHED`

#### Scenario: 非法状态被拒绝

- **WHEN** 参与者终止非 `PENDING` 交接
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

#### Scenario: 非参与者被拒绝

- **WHEN** 非参与者尝试终止交接
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 交接隐私保护

交接的时间窗口、公共区域与各确认时间 SHALL 仅对交接双方（信息发布者与申请人）可见，SHALL NOT 出现在公开接口、普通日志或任何公开信息中。

#### Scenario: 公开接口不泄露交接内容

- **WHEN** 任意用户查询公开信息列表或详情
- **THEN** 响应 SHALL NOT 包含任何交接的时间窗口、公共区域或确认时间
