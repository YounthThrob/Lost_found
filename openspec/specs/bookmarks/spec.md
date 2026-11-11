# bookmarks Specification

## Purpose
为平台补充「收藏」能力（PRODUCT.md REQ-010）：用户收藏/取消收藏展示中的信息，在「我的收藏」中查看并跳转详情；收藏仅自己可见、幂等不产生重复记录、收藏量不对外展示，被收藏信息状态变化后收藏仍保留但显示对应状态提示。

## Requirements

### Requirement: 收藏展示中的信息

用户 SHALL 能够对「展示中」（`PUBLISHED`/`VERIFYING`）的信息收藏。收藏操作 SHALL 幂等：重复收藏同一信息 SHALL NOT 产生重复记录。对不存在或非展示中的信息收藏 SHALL 返回 `NOT_FOUND`（HTTP 404）。

#### Scenario: 收藏成功

- **WHEN** 用户对展示中的信息发起收藏
- **THEN** 系统保存收藏关系并返回该收藏（含信息当前状态）

#### Scenario: 重复收藏不产生重复记录

- **WHEN** 用户再次收藏已收藏的信息
- **THEN** 系统返回既有收藏且不产生重复记录

#### Scenario: 非展示中信息不可收藏

- **WHEN** 用户对不存在或非展示中的信息发起收藏
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）且不保存

### Requirement: 取消收藏

用户 SHALL 能够取消收藏。取消收藏操作 SHALL 幂等：未收藏时取消也 SHALL 返回成功（HTTP 200）。

#### Scenario: 取消收藏成功

- **WHEN** 用户对已收藏信息发起取消收藏
- **THEN** 系统移除收藏关系并返回成功

#### Scenario: 重复取消收藏幂等

- **WHEN** 用户对未收藏信息发起取消收藏
- **THEN** 系统返回成功（HTTP 200）

### Requirement: 查看我的收藏

用户 SHALL 能够分页查看自己的收藏列表，按收藏时间倒序。每条收藏 SHALL 包含信息当前状态（`post.status`），用于标注已下架/已关闭等信息状态变化。

#### Scenario: 查看收藏列表

- **WHEN** 用户查询我的收藏
- **THEN** 系统返回该用户的收藏列表与分页信息，每条含信息当前状态

#### Scenario: 被收藏信息状态变化仍保留

- **WHEN** 被收藏信息关闭或不可见后用户查询收藏列表
- **THEN** 该收藏 SHALL 仍出现在列表，且其信息状态反映当前状态

### Requirement: 收藏可见性与数量边界

收藏 SHALL 仅对本人可见，SHALL NOT 进入公开搜索、信息详情或任何其他用户可访问的接口。收藏数量 SHALL NOT 对外展示，SHALL NOT 进入账户可信信息。

#### Scenario: 收藏不对外公开

- **WHEN** 任意用户查询公开搜索、信息详情或他人收藏
- **THEN** 响应 SHALL NOT 包含他人的收藏记录或收藏数量
