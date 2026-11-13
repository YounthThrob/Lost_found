# user-profile Specification

## Purpose

为已登录用户提供昵称与头像的个人资料能力，实现微信授权登录的「昵称/头像」部分。昵称与头像为用户或微信提供、仅本人可见，不构成任何身份、校方或官方认证。

## Requirements

### Requirement: 用户设置昵称

已登录用户 SHALL 能够通过 `PUT /me/profile` 设置昵称。系统 SHALL 去除首尾空白并校验昵称非空且长度不超过 30 个字符，校验通过后保存并返回最新资料。

#### Scenario: 设置昵称成功

- **WHEN** 已登录用户提交非空且长度不超过 30 的昵称
- **THEN** 系统返回 200，响应 data 的 `nickname` 为该昵称，`avatarUrl` 为当前头像地址（未设置时为 `null`）

#### Scenario: 空昵称被拒绝

- **WHEN** 用户提交空字符串或仅空白字符的昵称
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且昵称不变

#### Scenario: 超长昵称被拒绝

- **WHEN** 用户提交长度超过 30 个字符的昵称
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且昵称不变

### Requirement: 用户上传头像

已登录用户 SHALL 能够通过 `POST /me/avatar`（multipart，字段名 `file`）上传头像。系统 SHALL 校验类型为 `image/jpeg`、`image/png` 或 `image/webp`，且大小不超过 10MB；校验通过后写入存储并返回可公开访问的头像地址。重新上传 SHALL 覆盖旧头像。

#### Scenario: 上传合法头像成功

- **WHEN** 已登录用户上传类型与大小合法的图片
- **THEN** 系统返回 200，响应 data 的 `avatarUrl` 以 `/media/` 开头

#### Scenario: 非支持类型被拒绝

- **WHEN** 用户上传非 jpeg/png/webp 类型的图片
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且头像不变

#### Scenario: 超大图片被拒绝

- **WHEN** 用户上传超过 10MB 的图片
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且头像不变

#### Scenario: 重新上传覆盖旧头像

- **WHEN** 已设置头像的用户再次上传合法头像
- **THEN** 系统返回新的 `avatarUrl`，旧头像不再作为当前头像返回

### Requirement: 账户信息返回昵称与头像

`GET /me` SHALL 返回 `nickname` 与 `avatarUrl` 两个字段，未设置时均为 `null`。

#### Scenario: 未设置时返回 null

- **WHEN** 新用户查询 `/me`
- **THEN** 响应 data 的 `nickname` 与 `avatarUrl` 均为 `null`

#### Scenario: 设置后返回对应值

- **WHEN** 已设置昵称与头像的用户查询 `/me`
- **THEN** 响应 data 的 `nickname` 为该昵称，`avatarUrl` 为当前头像地址

### Requirement: 昵称与头像不进入公开接口

昵称与头像 SHALL NOT 出现在公开的物品列表、物品详情、搜索等响应中，仅通过 `GET /me` 返回给本人。

#### Scenario: 公开详情不泄露昵称与头像

- **WHEN** 任一用户（含匿名用户）查询已发布物品的详情
- **THEN** 响应 data SHALL NOT 包含 `nickname` 或 `avatarUrl` 字段
