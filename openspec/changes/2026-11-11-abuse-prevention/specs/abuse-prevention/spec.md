# Spec Delta

## Purpose

为平台补充滥用防护的 MVP 切片：对认领、发布、消息与举报等受限操作实施按用户维度的固定窗口频率限制，并对处于限制状态的账户拒绝发布/留言/认领；同时确保风控错误响应不泄露阈值、窗口或规则细节。审计日志与运营触发封禁属后续 change。

## ADDED Requirements

### Requirement: 认领申请频率限制

系统 SHALL 对认领申请按用户维度实施固定窗口频率限制；同一用户在窗口内提交的申请达到上限后再次提交时，系统 SHALL 返回 `CLAIM_RATE_LIMITED`（HTTP 429）并拒绝该次请求。

#### Scenario: 超过上限被拒绝

- **WHEN** 同一用户在固定窗口内提交的认领申请达到上限后再次提交
- **THEN** 系统返回 `CLAIM_RATE_LIMITED`（HTTP 429）且不保存该申请

#### Scenario: 未超过上限不受影响

- **WHEN** 同一用户在固定窗口内提交的认领申请未达到上限
- **THEN** 系统按既有规则处理该申请，不触发频率限制

### Requirement: 受限操作频率限制

系统 SHALL 对发布信息、发送消息与提交举报按用户维度实施固定窗口频率限制，超过上限时返回 `RATE_LIMITED`（HTTP 429）。不同操作 SHALL 独立计数。

#### Scenario: 发布超过上限被拒绝

- **WHEN** 同一用户在固定窗口内创建信息达到上限后再次创建
- **THEN** 系统返回 `RATE_LIMITED`（HTTP 429）且不保存该信息

#### Scenario: 不同操作独立计数

- **WHEN** 用户对某一操作达到上限
- **THEN** 该用户的其他操作不受该操作上限影响

### Requirement: 账户限制

系统 SHALL 对处于限制状态的账户拒绝受限操作（发布、留言、认领），返回 `ACCOUNT_RESTRICTED`（HTTP 403）且不执行该操作。限制状态由运营/管理动作设置，该触发端点属后续 change。

#### Scenario: 受限账户被拒绝

- **WHEN** 处于限制状态的账户尝试发布、留言或认领
- **THEN** 系统返回 `ACCOUNT_RESTRICTED`（HTTP 403）且不执行该操作

#### Scenario: 正常账户不受影响

- **WHEN** 状态为 ACTIVE 的账户执行受限操作
- **THEN** 系统按既有规则处理，不返回账户限制错误

### Requirement: 风控细节保护

频率限制与账户限制的错误响应 SHALL NOT 泄露阈值、窗口长度或任何风控规则细节；响应 SHALL 仅包含错误码与通用说明。

#### Scenario: 错误响应不含风控细节

- **WHEN** 用户触发频率限制或账户限制
- **THEN** 响应 SHALL 仅包含错误码与通用 message，不含阈值、窗口或规则细节
