# user-account Specification

## Purpose

为用户建立基于微信登录的平台账户与会话，作为所有需要登录能力（发布、认领、沟通、交接）的身份基础。

## Requirements

### Requirement: 用户通过微信授权登录

用户 SHALL 能够通过微信小程序授权登录建立平台会话。服务端 SHALL 调用微信 code2session 校验临时 code，并在平台账户不存在时创建账户、随后建立会话。

#### Scenario: 有效 code 建立会话

- **WHEN** 客户端以有效的微信 code 请求登录
- **THEN** 系统建立平台会话并返回会话凭证

#### Scenario: 登录响应不泄露微信敏感凭据

- **WHEN** 登录成功
- **THEN** 响应 SHALL NOT 包含 `session_key` 或 `openid`

### Requirement: 受保护接口要求有效会话

需要登录的接口 SHALL 校验请求会话的有效性。

#### Scenario: 会话无效或过期被拒绝

- **WHEN** 请求携带无效或已过期的会话
- **THEN** 系统返回 `SESSION_EXPIRED`（HTTP 401）且不执行业务操作

#### Scenario: 有效会话正常放行

- **WHEN** 请求携带有效会话
- **THEN** 系统正常处理该请求

### Requirement: 登录失败返回明确错误

当 code 无效、已过期，或微信服务调用失败时，系统 SHALL 返回明确错误且不建立会话。

#### Scenario: code 无效或过期

- **WHEN** 客户端提交无效或已过期的微信 code
- **THEN** 系统返回 `WECHAT_CODE_INVALID`（HTTP 401）且不建立会话

#### Scenario: 微信服务不可用

- **WHEN** 服务端调用微信 code2session 失败
- **THEN** 系统返回 `INTERNAL_ERROR`（HTTP 500）且不建立会话
