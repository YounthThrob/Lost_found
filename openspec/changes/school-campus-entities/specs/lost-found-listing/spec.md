# Spec Delta

## MODIFIED Requirements

### Requirement: 用户创建寻物或招领草稿

已确认协议的用户 SHALL 能够创建寻物（LOST）或招领（FOUND）信息草稿。系统 SHALL 校验类型、标题、分类、公开描述等必填字段及长度限制；schoolId 未显式提供时默认继承作者档案的学校，作者未设置学校且未提供 schoolId 时 SHALL 拒绝创建。草稿 SHALL NOT 出现在公开搜索结果中。

#### Scenario: 创建草稿成功

- **WHEN** 已确认协议且已设置学校的用户提交合法的类型、标题、分类、公开描述（未显式提供 schoolId）
- **THEN** 系统保存草稿，schoolId 继承作者档案，返回 `status` 为 `DRAFT`

#### Scenario: 未确认协议被拒绝

- **WHEN** 未确认协议的用户尝试创建信息
- **THEN** 系统返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不保存

#### Scenario: 字段校验失败

- **WHEN** 用户提交空标题、非法类型、超长字段或无效 schoolId
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 显式指定学校覆盖继承

- **WHEN** 用户提交合法的 schoolId（可与作者档案不同）
- **THEN** 系统以该 schoolId 保存草稿

#### Scenario: 未设置学校且未提供 schoolId 被拒绝

- **WHEN** 作者未设置学校且创建时未提供 schoolId
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 搜索与筛选已发布信息

用户 SHALL 能够搜索已发布信息，按关键词、类型、分类、schoolId 与 campusId 筛选，结果分页返回。系统 SHALL 仅返回 `PUBLISHED` 或 `VERIFYING` 信息，SHALL NOT 返回草稿、已关闭、已下架或待交接信息，或任何私密字段；每条结果附公开图片。

#### Scenario: 默认搜索仅返回已发布信息

- **WHEN** 用户不带筛选条件搜索
- **THEN** 系统仅返回 `PUBLISHED` 或 `VERIFYING` 信息，按创建时间倒序

#### Scenario: 按 schoolId 与 campusId 筛选

- **WHEN** 用户以 schoolId 或 campusId 筛选
- **THEN** 系统仅返回匹配的公开可见信息

#### Scenario: 按关键词与类型筛选

- **WHEN** 用户以关键词或类型筛选
- **THEN** 系统仅返回匹配的公开可见信息

#### Scenario: 搜索结果附图片

- **WHEN** 用户搜索返回公开信息
- **THEN** 每条结果的 `images` 列表包含其公开图片

#### Scenario: 分页返回

- **WHEN** 用户指定 `page` 与 `pageSize`
- **THEN** 系统返回 `items` 与 `pagination{page,pageSize,total,hasMore}`
