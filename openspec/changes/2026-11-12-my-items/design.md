# 设计：我的发布与我的申请

## 决策

1. **复用现有视图**：两个列表分别返回 `ItemPostView` 与 `ClaimView` 列表，复用 `ItemPostListResponse`/`ClaimListResponse`，不引入新 schema。
2. **自作用域隔离**：`GET /me/posts` 仅返回 `authorId == 当前用户` 的信息；`GET /me/claims` 仅返回 `applicantId == 当前用户` 的申请。无跨用户数据。
3. **不过滤状态**：`/me/posts` 返回全部状态（含 DRAFT/REJECTED/CLOSED/REMOVED），以支撑草稿续编与关闭；`/me/claims` 返回全部状态。
4. **排序与分页**：均按 `createdAt` 倒序、`id` 升序兜底，复用现有分页 `page/pageSize`（默认 20、上限 100）。
5. **鉴权**：仅需登录（已认证），不要求已确认协议，与 `GET /posts`、`GET /posts/{postId}/claims` 一致。

## 隐私

- `/me/claims` 返回的 `statement` 为申请人本人提交的归属证明，属于本人可见范围，非公开接口；不进入搜索、埋点或缓存。
