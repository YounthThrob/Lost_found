# Spec Delta

## Purpose

引入认领核验引入的信息新状态：`PUBLISHED → VERIFYING`（有申请进入核验）与 `VERIFYING → PENDING_HANDOVER`（申请被接受）。公开可见范围由「仅 PUBLISHED」扩展为「PUBLISHED 或 VERIFYING」。

## MODIFIED Requirements

### Requirement: 查询信息详情与可见性

用户 SHALL 能够查询信息详情。系统 SHALL 对所有人返回 `PUBLISHED` 或 `VERIFYING` 信息的详情；其他状态信息仅对作者可见；否则返回 `NOT_FOUND`（HTTP 404）。

#### Scenario: 已发布详情对所有人可见

- **WHEN** 任意已登录用户查询一条 `PUBLISHED` 信息
- **THEN** 系统返回该信息详情

#### Scenario: 核验中详情对所有人可见

- **WHEN** 任意已登录用户查询一条 `VERIFYING` 信息
- **THEN** 系统返回该信息详情

#### Scenario: 草稿仅作者可见

- **WHEN** 作者查询自己的 `DRAFT` 信息
- **THEN** 系统返回该信息详情

#### Scenario: 草稿对非作者不可见

- **WHEN** 非作者查询他人的 `DRAFT` 信息
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 搜索与筛选已发布信息

用户 SHALL 能够搜索已发布信息，按关键词、类型、分类、学校与校区筛选，结果分页返回。系统 SHALL 仅返回 `PUBLISHED` 或 `VERIFYING` 信息，SHALL NOT 返回草稿、已关闭、已下架或待交接信息，或任何私密字段。

#### Scenario: 默认搜索仅返回已发布信息

- **WHEN** 用户不带筛选条件搜索
- **THEN** 系统仅返回 `PUBLISHED` 或 `VERIFYING` 信息，按创建时间倒序

#### Scenario: 按关键词与类型筛选

- **WHEN** 用户以关键词或类型筛选
- **THEN** 系统仅返回匹配的公开可见信息

#### Scenario: 分页返回

- **WHEN** 用户指定 `page` 与 `pageSize`
- **THEN** 系统返回 `items` 与 `pagination{page,pageSize,total,hasMore}`
