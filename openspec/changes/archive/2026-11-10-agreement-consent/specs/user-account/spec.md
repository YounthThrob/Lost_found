# Spec Delta

## Purpose

在既有微信登录账户之上，补充用户协议与非官方风险提示的确认状态，以及据此对受限操作的门控，兑现 REQ-001 的首次确认约束。

## ADDED Requirements

### Requirement: 用户确认协议与风险提示

系统 SHALL 记录账户的协议确认状态，新账户默认未确认。用户 SHALL 能够通过确认接口确认用户协议、隐私说明与非官方平台提示，确认动作幂等。

#### Scenario: 未确认用户确认协议

- **WHEN** 已登录且未确认协议的用户调用确认接口
- **THEN** 系统记录确认状态与时间，返回 `agreementAccepted: true`

#### Scenario: 重复确认幂等

- **WHEN** 已确认协议的用户再次调用确认接口
- **THEN** 系统仍返回 200 与 `agreementAccepted: true`，且不覆盖首次确认时间

### Requirement: 账户信息返回协议确认状态

登录响应与 `GET /me` SHALL 返回 `agreementAccepted`，供客户端决定是否展示协议确认页。

#### Scenario: 新用户未确认

- **WHEN** 新用户登录或查询 `/me`
- **THEN** 响应中 `agreementAccepted` 为 `false`

#### Scenario: 已确认用户

- **WHEN** 已确认协议的用户查询 `/me`
- **THEN** 响应中 `agreementAccepted` 为 `true`

### Requirement: 未确认协议的用户受限

未确认协议的用户执行受限操作（发布、认领、留言）时，系统 SHALL 返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不执行业务操作。

#### Scenario: 未确认协议执行受限操作被拒绝

- **WHEN** 未确认协议的用户尝试发布、认领或留言
- **THEN** 系统返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不执行业务操作
