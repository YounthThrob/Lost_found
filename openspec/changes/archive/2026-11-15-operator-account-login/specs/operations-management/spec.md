# Spec Delta

## ADDED Requirements

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
