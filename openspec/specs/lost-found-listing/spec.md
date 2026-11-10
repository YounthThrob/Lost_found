# lost-found-listing Specification

## Purpose
定义寻物/招领信息从创建、编辑、发布到关闭的完整生命周期与公开搜索能力，确保状态迁移由服务端校验、公开接口仅暴露已发布信息。

## Requirements

### Requirement: 用户创建寻物或招领草稿

已确认协议的用户 SHALL 能够创建寻物（LOST）或招领（FOUND）信息草稿。系统 SHALL 校验类型、标题、分类、公开描述与学校等必填字段及长度限制，草稿 SHALL NOT 出现在公开搜索结果中。

#### Scenario: 创建草稿成功

- **WHEN** 已确认协议的用户提交合法的类型、标题、分类、公开描述与学校
- **THEN** 系统保存草稿并返回 `status` 为 `DRAFT`

#### Scenario: 未确认协议被拒绝

- **WHEN** 未确认协议的用户尝试创建信息
- **THEN** 系统返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不保存

#### Scenario: 字段校验失败

- **WHEN** 用户提交空标题、非法类型或超长字段
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 用户编辑草稿

作者 SHALL 能够编辑自己的草稿。系统 SHALL 仅允许编辑 `DRAFT` 状态的信息，非作者不可编辑。

#### Scenario: 编辑草稿成功

- **WHEN** 作者对 `DRAFT` 信息提交合法的编辑内容
- **THEN** 系统保存修改并返回更新后的信息

#### Scenario: 非草稿不可编辑

- **WHEN** 作者尝试编辑非 `DRAFT` 状态（已发布或已关闭）的信息
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不修改

#### Scenario: 非作者不可编辑

- **WHEN** 非作者尝试编辑他人的信息
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）且不修改

### Requirement: 发布信息并校验状态迁移

作者 SHALL 能够提交草稿发布。系统 SHALL 在服务端校验状态迁移：仅 `DRAFT` 可迁移至 `PENDING_REVIEW`，随后自动通过审核迁移至 `PUBLISHED`。客户端 SHALL NOT 任意指定目标状态。

#### Scenario: 发布成功

- **WHEN** 作者对 `DRAFT` 信息提交发布
- **THEN** 系统将状态迁移为 `PUBLISHED` 并返回更新后的信息

#### Scenario: 重复发布被拒绝

- **WHEN** 作者对非 `DRAFT` 状态（如已发布）的信息再次提交发布
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

### Requirement: 关闭已发布信息并校验状态迁移

作者 SHALL 能够关闭自己已发布的信息。系统 SHALL 在服务端校验状态迁移：仅 `PUBLISHED` 可迁移至 `CLOSED`。已关闭信息 SHALL NOT 出现在公开搜索结果中。

#### Scenario: 关闭成功

- **WHEN** 作者对 `PUBLISHED` 信息提交关闭
- **THEN** 系统将状态迁移为 `CLOSED` 并返回更新后的信息

#### Scenario: 非法关闭被拒绝

- **WHEN** 作者对非 `PUBLISHED` 状态（如草稿）的信息提交关闭
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不改变状态

#### Scenario: 已关闭信息不出现在搜索中

- **WHEN** 信息已关闭后执行搜索
- **THEN** 搜索结果 SHALL NOT 包含该信息

### Requirement: 查询信息详情与可见性

用户 SHALL 能够查询信息详情。系统 SHALL 对所有人返回已发布（`PUBLISHED`）信息的详情；非已发布信息仅对作者可见；其他情况返回 `NOT_FOUND`（HTTP 404）。

#### Scenario: 已发布详情对所有人可见

- **WHEN** 任意已登录用户查询一条 `PUBLISHED` 信息
- **THEN** 系统返回该信息详情

#### Scenario: 草稿仅作者可见

- **WHEN** 作者查询自己的 `DRAFT` 信息
- **THEN** 系统返回该信息详情

#### Scenario: 草稿对非作者不可见

- **WHEN** 非作者查询他人的 `DRAFT` 信息
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 搜索与筛选已发布信息

用户 SHALL 能够搜索已发布信息，按关键词、类型、分类、学校与校区筛选，结果分页返回。系统 SHALL 仅返回 `PUBLISHED` 信息，SHALL NOT 返回草稿、已关闭信息或任何私密字段。

#### Scenario: 默认搜索仅返回已发布信息

- **WHEN** 用户不带筛选条件搜索
- **THEN** 系统仅返回 `PUBLISHED` 信息，按创建时间倒序

#### Scenario: 按关键词与类型筛选

- **WHEN** 用户以关键词或类型筛选
- **THEN** 系统仅返回匹配的已发布信息

#### Scenario: 分页返回

- **WHEN** 用户指定 `page` 与 `pageSize`
- **THEN** 系统返回 `items` 与 `pagination{page,pageSize,total,hasMore}`
