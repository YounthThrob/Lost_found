# operations-management Specification

## Purpose
为平台补充「运营管理」能力（PRODUCT.md REQ-008）：建立运营身份与鉴权，运营可下架违规信息、限制/解除限制账户，并查看举报与审计日志；每个管理动作记录操作者、事件类型、目标、原因与时间，满足 §18「举报、拉黑、下架和账号限制端到端运行」「运营管理动作具有审计记录」。

## Requirements

### Requirement: 运营身份鉴权

运营管理端点 SHALL 仅对 `role = OPERATOR` 的用户开放。非运营用户访问 SHALL 返回 `OPERATOR_REQUIRED`（HTTP 403）且不执行任何管理动作。

#### Scenario: 运营访问管理端点

- **WHEN** `role = OPERATOR` 的用户访问运营管理端点
- **THEN** 系统执行对应管理动作

#### Scenario: 非运营访问被拒绝

- **WHEN** `role = USER` 的用户访问运营管理端点
- **THEN** 系统返回 `OPERATOR_REQUIRED`（HTTP 403）且不执行管理动作

### Requirement: 下架信息

运营 SHALL 能够对「公开状态」（`PUBLISHED`/`VERIFYING`/`PENDING_HANDOVER`）的信息下架，状态迁移为 `REMOVED`。对非公开状态的信息下架 SHALL 返回 `INVALID_STATE_TRANSITION`（HTTP 409）。下架信息 SHALL NOT 出现在公开搜索或信息详情中。

#### Scenario: 下架公开信息

- **WHEN** 运营对公开状态的信息执行下架
- **THEN** 系统将状态迁移为 `REMOVED` 并返回更新后的信息

#### Scenario: 下架后不出现在搜索

- **WHEN** 信息被下架后执行公开搜索
- **THEN** 搜索结果 SHALL NOT 包含该信息

#### Scenario: 下架非法状态被拒绝

- **WHEN** 运营对非公开状态（如 `CLOSED`、`REMOVED`）的信息执行下架
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

### Requirement: 限制与解除限制账户

运营 SHALL 能够限制账户（状态迁移为 `RESTRICTED`）与解除限制（状态迁移为 `ACTIVE`）。处于 `RESTRICTED` 的账户 SHALL 被拒绝执行受限操作（发布、留言、认领），返回 `ACCOUNT_RESTRICTED`（HTTP 403）。

#### Scenario: 限制账户

- **WHEN** 运营对账户执行限制
- **THEN** 系统将账户状态置为 `RESTRICTED` 并返回更新后的状态

#### Scenario: 受限账户被阻断

- **WHEN** 处于 `RESTRICTED` 的账户尝试发布、留言或认领
- **THEN** 系统返回 `ACCOUNT_RESTRICTED`（HTTP 403）且不执行该操作

#### Scenario: 解除限制

- **WHEN** 运营对已限制账户执行解除限制
- **THEN** 系统将账户状态置为 `ACTIVE`，该账户恢复受限操作能力

### Requirement: 查看举报列表

运营 SHALL 能够分页查看全部举报（不限提交者），按提交时间倒序。

#### Scenario: 运营查看全部举报

- **WHEN** 运营查询举报列表
- **THEN** 系统返回全部举报的分页列表

### Requirement: 管理动作审计

每个管理动作（下架、限制、解除限制）SHALL 写入一条 `AuditEvent`，记录操作者、事件类型、目标类型与标识、原因与时间。运营 SHALL 能够分页查看审计日志。

#### Scenario: 管理动作写入审计

- **WHEN** 运营执行下架、限制或解除限制
- **THEN** 系统写入对应的 `AuditEvent` 且其 `metadata` 包含原因

#### Scenario: 运营查看审计日志

- **WHEN** 运营查询审计日志
- **THEN** 系统返回按时间倒序的审计事件分页列表

#### Scenario: 非运营不可查看审计日志

- **WHEN** 非运营用户查询审计日志
- **THEN** 系统返回 `OPERATOR_REQUIRED`（HTTP 403）

### Requirement: 运营通过账号密码登录

运营 SHALL 能够通过用户名与密码建立运营会话；服务端 SHALL 校验凭据并为 `role = OPERATOR` 的账户签发会话凭证。运营密码 SHALL 仅以单向哈希存储，任何接口 SHALL NOT 返回密码或其哈希。

#### Scenario: 有效凭据建立运营会话

- **WHEN** 客户端以正确的运营用户名与密码请求登录
- **THEN** 系统返回会话凭证（HTTP 200），该凭证可访问运营管理端点

#### Scenario: 用户名或密码错误

- **WHEN** 客户端以错误的用户名或密码请求登录
- **THEN** 系统返回 `INVALID_CREDENTIALS`（HTTP 401）且不建立会话

#### Scenario: 登录响应不泄露密码

- **WHEN** 登录成功或失败
- **THEN** 响应 SHALL NOT 包含密码或其哈希
