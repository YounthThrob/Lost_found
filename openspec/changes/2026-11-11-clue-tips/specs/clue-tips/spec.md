# Spec Delta

## Purpose

为平台补充「线索提供」能力（PRODUCT.md REQ-009）：用户对展示中的寻物信息提交线索，失主查看并判定线索有效/无效；线索仅失主可见、不公开，提交受内容审核与频率限制约束，状态迁移由服务端校验。

## ADDED Requirements

### Requirement: 向寻物信息提交线索

已确认协议的用户 SHALL 能够向「展示中」的寻物（LOST）信息提交线索。系统 SHALL 校验：仅 LOST 且公开可见（`PUBLISHED`/`VERIFYING`）的信息可提交；对招领（FOUND）信息提交线索 SHALL 返回 `CLUE_NOT_ALLOWED`（HTTP 409）。线索内容 SHALL 非空且不超过 2000 字符。线索提交 SHALL 受频率限制约束，超限 SHALL 返回 `RATE_LIMITED`（HTTP 429）。

#### Scenario: 提交线索成功

- **WHEN** 已确认协议的用户向展示中的 LOST 信息提交非空、合法的线索内容
- **THEN** 系统保存线索并返回 `status` 为 `SUBMITTED`

#### Scenario: 对招领信息提交被拒绝

- **WHEN** 用户对 FOUND 信息提交线索
- **THEN** 系统返回 `CLUE_NOT_ALLOWED`（HTTP 409）且不保存

#### Scenario: 未确认协议被拒绝

- **WHEN** 未确认协议的用户提交线索
- **THEN** 系统返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不保存

#### Scenario: 内容为空或超长被拒绝

- **WHEN** 用户提交空线索或超过 2000 字符的线索内容
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 线索内容审核

线索内容 SHALL 在提交时通过违禁关键词检测。命中违禁关键词的线索 SHALL 保存为 `REJECTED`。

#### Scenario: 命中违禁词被拒绝

- **WHEN** 线索内容包含违禁关键词
- **THEN** 系统保存线索为 `REJECTED`

#### Scenario: 未命中违禁词通过

- **WHEN** 线索内容未包含违禁关键词
- **THEN** 系统保存线索为 `SUBMITTED`

### Requirement: 失主查看待处理线索

失主 SHALL 能够分页查看自己信息收到的待处理线索（`status = SUBMITTED`），按提交时间倒序。`REJECTED`、`ACCEPTED`、`CLOSED` 线索 SHALL NOT 出现在该列表。非失主查询 SHALL 返回 `NOT_FOUND`（HTTP 404）。

#### Scenario: 失主查询成功

- **WHEN** 失主查询自己信息收到的线索
- **THEN** 系统返回待处理线索列表与分页信息

#### Scenario: 非失主查询被拒绝

- **WHEN** 非失主查询该信息的线索
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

#### Scenario: 已处理线索不出现在待处理列表

- **WHEN** 线索被标记有效或无效后失主查询待处理线索
- **THEN** 该线索 SHALL NOT 出现在结果中

### Requirement: 标记线索有效或无效

失主 SHALL 能够对自己收到的线索标记有效或无效。标记有效 SHALL 迁移 `SUBMITTED → ACCEPTED`，标记无效 SHALL 迁移 `SUBMITTED → CLOSED`。状态迁移 SHALL 由服务端校验：仅 `SUBMITTED` 可被判定，已处理线索重复判定 SHALL 返回 `INVALID_STATE_TRANSITION`（HTTP 409）。非失主操作 SHALL 返回 `NOT_FOUND`（HTTP 404）。

#### Scenario: 标记有效

- **WHEN** 失主对 `SUBMITTED` 线索标记有效
- **THEN** 系统将状态迁移为 `ACCEPTED` 并返回更新后的线索

#### Scenario: 标记无效

- **WHEN** 失主对 `SUBMITTED` 线索标记无效
- **THEN** 系统将状态迁移为 `CLOSED` 并返回更新后的线索

#### Scenario: 已处理线索重复判定被拒绝

- **WHEN** 失主对非 `SUBMITTED` 状态的线索再次判定
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

#### Scenario: 非失主判定被拒绝

- **WHEN** 非失主尝试判定线索
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 线索可见性边界

线索 SHALL 仅对失主（及后续运营人员）可见，SHALL NOT 进入公开搜索、公开信息详情或任何非失主可访问的接口。

#### Scenario: 线索不对外公开

- **WHEN** 任意用户查询公开搜索或信息详情
- **THEN** 响应 SHALL NOT 包含任何线索内容或线索存在性信息
