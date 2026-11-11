# Design

## Context

- 已有 `lost-found-listing`（信息发布/搜索，`PUBLISHED`/`VERIFYING` 为公开可见状态）、`safe-communication`（幂等拉黑/解除拉黑模式）、`user-account`（鉴权）。
- PRODUCT.md §11 已列收藏 3 端点与 `Bookmark` 数据模型（user_id、post_id、created_at 联合唯一）；§9.3 明确「收藏为布尔型关系，无状态机」。
- PRODUCT.md REQ-010：收藏/取消收藏展示中的信息、收藏仅自己可见、被收藏信息下架/关闭后收藏仍保留但显示状态提示、幂等不产生重复记录、收藏量不对外展示。

## Goals / Non-Goals

**Goals:**

- 对「展示中」的信息收藏（幂等新增，重复收藏不产生重复记录），取消收藏（幂等删除）。
- 我的收藏分页列表，每条含信息当前状态（`post.status`），供客户端标注「已下架/已关闭」并阻止进入详情。
- 收藏仅自己可见；收藏量不对外展示、不进入任何公开接口。
- 两后端等价实现。

**Non-Goals:**

- 不实现收藏列表内的模糊过滤/排序选项（MVP 仅按收藏时间倒序）。
- 不实现「收藏数量」计数接口或对外展示。
- 不实现收藏信息的推送/通知。
- 不实现被收藏信息下架后的自动清理（收藏保留，见 REQ-010）。

## Decisions

1. **端点与语义**：`POST /posts/{postId}/bookmark` = 收藏（幂等新增，已收藏时返回既有收藏，不产生重复记录）；`DELETE /posts/{postId}/bookmark` = 取消收藏（幂等删除，未收藏时也返回 200）；`GET /me/bookmarks` = 我的收藏（分页）。REQ-010「再次点击收藏→取消收藏」由客户端实现：按钮在已收藏态语义为「取消」，点击发送 DELETE；服务端保持 REST 幂等语义，与 §11 端点表、§9.3「仅在取消收藏时移除」一致。

2. **可收藏性校验**：仅「展示中」（`PUBLISHED`/`VERIFYING`）信息可收藏；信息不存在或非展示中返回 `NOT_FOUND`（404）。与线索/认领的可提交性判定共用 `listVisible` 语义。

3. **取消收藏不做存在性校验**：DELETE 为幂等删除，仅移除关系记录；不校验信息可见性（被关闭的信息也应能取消收藏）。

4. **收藏列表返回信息当前状态**：每条收藏返回 `{postId, createdAt, post}`，其中 `post` 为 `ItemPostView`（含当前 `status`）。客户端据 `status` 标注「已下架/已关闭」并阻止进入详情（非展示中信息详情本就返回 404）。

5. **可见性与数量边界**：收藏列表仅 `GET /me/bookmarks` 返回本人收藏；不加入公开搜索/信息详情，不提供收藏量字段。`bookmark` 关系仅存 user_id/post_id/created_at，不承载任何私有信息。

6. **无协议守卫、无限流**：收藏是轻量个人操作，不发布内容，无需 `@RequiresAgreement`（与拉黑一致）；不接入限流（与拉黑一致）。

7. **契约测试**：复用 mock 用户（`valid-code` 为信息发布者，`valid-code-2` 为收藏者）；探针覆盖：收藏成功、重复收藏幂等、非展示中拒绝、收藏列表（含状态）、取消收藏、取消幂等、收藏隔离。

## Risks / Trade-offs

- [幂等语义 vs toggle] 服务端采用 POST=收藏/DELETE=取消收藏的 REST 语义，toggle 由客户端实现；若后续要求服务端 POST 自动 toggle，需另作契约变更。
- [无持久化] 收藏内存存储，重启即失；随持久化 change 落地。
- [无下架联动] 信息下架（REMOVED）尚未实现（属运营管理 change）；当前「已关闭」由 CLOSED 等状态体现。

## Migration Plan

- 无既有数据迁移（内存存储）。新增 3 个端点与 5 个 schema 为契约增量，无错误码/限流变更。
