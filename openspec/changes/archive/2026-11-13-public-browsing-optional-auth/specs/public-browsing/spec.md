# Spec Delta

## Purpose

让用户无需登录即可浏览招领/寻物信息与广场统计，仅在写操作时要求登录；公开只读端点支持可选登录，缺失或无效令牌一律视为匿名而非 401。

## ADDED Requirements

### Requirement: 公开浏览（匿名读取）

系统 SHALL 允许未登录用户访问 `GET /posts`、`GET /posts/{postId}` 与 `GET /stats`，并仅返回公开字段。这三个端点 SHALL 支持可选 Bearer 令牌：令牌缺失、无效或过期时 SHALL 视为匿名而非返回 401；有效令牌可保留既有语义（如作者可查看自己的草稿详情）。

#### Scenario: 未登录浏览列表

- **WHEN** 未登录用户请求 `GET /posts`
- **THEN** 系统返回 200，仅包含状态为 PUBLISHED 或 VERIFYING 的信息

#### Scenario: 未登录查看公开详情

- **WHEN** 未登录用户请求 `GET /posts/{postId}` 且该信息为公开状态
- **THEN** 系统返回 200，仅包含公开字段

#### Scenario: 未登录查看非公开详情不泄露存在性

- **WHEN** 未登录用户请求 `GET /posts/{postId}` 且该信息非公开状态
- **THEN** 系统返回 404，SHALL NOT 泄露该信息是否存在

#### Scenario: 未登录查询统计

- **WHEN** 未登录用户请求 `GET /stats`
- **THEN** 系统返回 200，包含 `foundCount`、`lostCount`、`completedToday`

#### Scenario: 无效令牌按匿名处理

- **WHEN** 用户携带无效或过期令牌请求上述公开只读端点
- **THEN** 系统返回 200，视该用户为匿名而非返回 401

### Requirement: 写操作仍需登录

除公开只读端点、`POST /session/wechat` 与静态资源 `/media/**` 外，系统 SHALL 对其余端点保持登录要求：未登录用户 SHALL 收到 401 `SESSION_EXPIRED`。

#### Scenario: 未登录执行写操作被拒绝

- **WHEN** 未登录用户请求写操作端点（如 `POST /posts`）
- **THEN** 系统返回 401，业务错误码为 `SESSION_EXPIRED`

### Requirement: 公开端点不泄露私密数据

公开只读端点 SHALL NOT 返回私密核验特征、认领证据或自由消息；响应 SHALL 仅包含公开字段（`id`、`type`、`category`、`title`、`publicDescription`、`school`、`campus`、`area`、`status`、`authorId`、`createdAt`、`updatedAt`、`images`）。

#### Scenario: 公开详情仅含公开字段

- **WHEN** 用户（含匿名）请求公开详情
- **THEN** 响应 SHALL NOT 包含私密核验特征、认领证据或自由消息
