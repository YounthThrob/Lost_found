# my-items Specification

## Purpose
为「我的」模块补充两个自作用域列表端点：用户查看自己发布的信息与自己作为申请人提交的申请（PRODUCT.md §6）。支撑客户端「我的发布」「我的申请」页面。

## Requirements

### Requirement: 我的发布列表

用户 SHALL 能够分页查看自己发布的全部信息（不分状态，含草稿与已关闭），按创建时间倒序。

#### Scenario: 查看我的发布

- **WHEN** 已登录用户查询 `GET /me/posts`
- **THEN** 系统返回该用户全部信息的 `ItemPostView` 分页列表，含 DRAFT 状态的信息

#### Scenario: 我的发布隔离

- **WHEN** 用户 A 查询 `GET /me/posts`
- **THEN** 返回结果 SHALL NOT 包含他人（`authorId != A`）的信息

### Requirement: 我的申请列表

用户 SHALL 能够分页查看自己作为申请人提交的全部认领申请，按创建时间倒序。

#### Scenario: 查看我的申请

- **WHEN** 已登录用户查询 `GET /me/claims`
- **THEN** 系统返回该用户作为申请人的全部 `ClaimView` 分页列表

#### Scenario: 我的申请隔离

- **WHEN** 用户 A 查询 `GET /me/claims`
- **THEN** 返回结果 SHALL NOT 包含他人（`applicantId != A`）的申请
