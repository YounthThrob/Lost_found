# content-moderation Specification

## Purpose
在发布时对公开内容做自动违禁检测并支持用户举报与举报查询，确保违规内容被拒绝展示、举报被记录，为运营处理提供数据基础。

## Requirements

### Requirement: 发布时自动检测违禁内容

系统 SHALL 在信息提交发布时对公开内容（标题与公开描述）做自动检测。内容命中违禁关键词 SHALL 迁移至 `REJECTED`，未命中 SHALL 迁移至 `PUBLISHED`。被拒绝的信息 SHALL NOT 出现在公开搜索中。

#### Scenario: 命中违禁词被拒绝

- **WHEN** 草稿的标题或公开描述包含违禁关键词
- **THEN** 发布后状态为 `REJECTED`

#### Scenario: 未命中违禁词通过

- **WHEN** 草稿内容未命中违禁关键词
- **THEN** 发布后状态为 `PUBLISHED`

#### Scenario: 被拒绝信息不出现在搜索中

- **WHEN** 信息被 `REJECTED` 后执行搜索
- **THEN** 搜索结果 SHALL NOT 包含该信息

### Requirement: 用户提交举报

用户 SHALL 能够举报信息或用户。举报 SHALL 包含举报对象类型、对象标识与举报分类，系统 SHALL 校验对象类型与对象标识合法。

#### Scenario: 举报成功

- **WHEN** 已登录用户提交对象类型、对象标识与分类均合法的举报
- **THEN** 系统保存举报并返回 `status` 为 `SUBMITTED`

#### Scenario: 非法对象类型被拒绝

- **WHEN** 用户提交非法的对象类型
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 缺少分类被拒绝

- **WHEN** 用户提交空举报分类
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 用户查看自己的举报

用户 SHALL 能够查看自己提交的举报列表，结果分页返回，按提交时间倒序。

#### Scenario: 查询自己的举报

- **WHEN** 用户查询自己的举报
- **THEN** 系统返回该用户提交的举报列表与分页信息
