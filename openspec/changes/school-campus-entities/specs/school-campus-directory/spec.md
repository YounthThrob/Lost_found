# Spec Delta

## Purpose

提供学校/校区的规范化实体目录：让用户从带 ID 与别名的目录中选择学校与校区，替代自由文本，并支持校区按需申请与运营审核。

## ADDED Requirements

### Requirement: 查询学校目录

系统 SHALL 提供预置学校目录供选择，用户 SHALL 能够按名称或别名做包含匹配查询，结果分页返回且仅包含已启用学校。

#### Scenario: 按名称搜索学校

- **WHEN** 用户以学校名称关键词查询目录
- **THEN** 系统返回名称或别名匹配该关键词的已启用学校列表

#### Scenario: 别名命中同一学校

- **WHEN** 用户以某学校的别名查询
- **THEN** 系统返回该学校实体（别名与正式名归一为同一学校）

#### Scenario: 目录仅含已启用学校

- **WHEN** 用户查询目录
- **THEN** 系统 SHALL NOT 返回已停用的学校

### Requirement: 查询学校校区

用户 SHALL 能够查询指定学校下的校区列表；系统 SHALL 仅返回已审核通过（ACTIVE）的校区。

#### Scenario: 查询校区成功

- **WHEN** 用户查询某已存在学校的校区
- **THEN** 系统返回该学校 ACTIVE 校区列表（可为空）

#### Scenario: 学校不存在

- **WHEN** 用户查询不存在学校的校区
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

#### Scenario: 待审核校区不可见

- **WHEN** 某校区处于 PENDING 待审核状态
- **THEN** 查询校区列表 SHALL NOT 包含该校区

### Requirement: 申请新增校区

用户 SHALL 能够为某学校申请新增校区；申请后校区进入 PENDING 状态，待运营审核。

#### Scenario: 申请校区成功

- **WHEN** 用户为某学校提交一个不与现有校区重名的新校区名称
- **THEN** 系统创建 PENDING 状态校区并返回其标识

#### Scenario: 校区重名被拒绝

- **WHEN** 用户申请与该校现有校区同名（含别名）的校区
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不创建

#### Scenario: 学校不存在

- **WHEN** 用户为不存在的学校申请校区
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 审核校区申请

运营 SHALL 能够审核校区申请，将 PENDING 校区置为 ACTIVE（通过）或 REJECTED（拒绝）；仅 ACTIVE 校区对普通用户可选。

#### Scenario: 审核通过

- **WHEN** 运营将某 PENDING 校区审核为通过
- **THEN** 该校区状态变为 ACTIVE 并开始对用户可选

#### Scenario: 审核拒绝

- **WHEN** 运营将某 PENDING 校区审核为拒绝
- **THEN** 该校区状态变为 REJECTED 且对用户不可选
