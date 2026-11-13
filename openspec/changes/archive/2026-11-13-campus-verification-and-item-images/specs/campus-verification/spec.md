# Spec Delta

## Purpose

为用户提供仍守「非官方」边界的校园邮箱核验流程，核验成功后提升校园身份的可信度信号，但不构成校方或官方认证。

## ADDED Requirements

### Requirement: 发起校园邮箱核验

已登录用户 SHALL 能够提交校园邮箱发起核验。系统 SHALL 校验邮箱格式，并在配置了 `CAMPUS_EMAIL_DOMAINS` 白名单时校验邮箱域名是否允许；MVP 使用 mock 邮件提供方，不真实发送邮件。

#### Scenario: 发起核验成功

- **WHEN** 用户提交格式合法的校园邮箱
- **THEN** 系统返回 200，响应 data 包含 `expiresInSeconds`（验证码有效期）

#### Scenario: 邮箱格式非法被拒绝

- **WHEN** 用户提交格式非法的邮箱
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不发起核验

#### Scenario: 邮箱域名不允许被拒绝

- **WHEN** 白名单非空且用户提交的邮箱域名不在白名单内
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不发起核验

### Requirement: 确认验证码并完成核验

用户 SHALL 能够提交邮箱与验证码完成核验。系统 SHALL 校验验证码与邮箱匹配且在有效期内；校验通过后 SHALL 将 `verificationStatus` 置为 `EMAIL_VERIFIED` 并记录 `verifiedEmail` 与 `emailVerifiedAt`。

#### Scenario: 核验成功

- **WHEN** 用户提交与发起一致的邮箱和正确验证码
- **THEN** 系统返回 200，`verificationStatus` 为 `EMAIL_VERIFIED`，`verifiedEmail` 为该邮箱

#### Scenario: 验证码错误或过期被拒绝

- **WHEN** 用户提交错误或过期的验证码
- **THEN** 系统返回 `VERIFICATION_CODE_INVALID`（HTTP 422）且核验状态不变

#### Scenario: 重新核验覆盖旧邮箱

- **WHEN** 已核验用户再次发起并确认新的校园邮箱
- **THEN** 系统覆盖 `verifiedEmail`，`verificationStatus` 仍为 `EMAIL_VERIFIED`
