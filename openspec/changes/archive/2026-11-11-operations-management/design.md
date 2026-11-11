# Design

## Context

- 已有 `user-account`（User 含 `status`，`restrict`/`unrestrict` 已存在但无触发端点）、`lost-found-listing`（信息状态机与公开可见性）、`content-moderation`（违禁检测 + 举报提交/查询）、`abuse-prevention`（限流 + 账户限制守卫，`RESTRICTED` 已阻断发布/留言/认领）。
- PRODUCT.md §4 定义「运营管理员」角色；REQ-008 要求运营可下架信息、限制账户，且每个管理动作记录操作者、原因、时间和影响范围；§9.1 定义 `任意公开状态 → 违规处理成立 → REMOVED`；§10 定义 `ModerationAction` 与 `AuditEvent`。
- 现状缺口：无运营身份/鉴权、无下架端点、无限制触发端点、无审计记录。

## Goals / Non-Goals

**Goals:**

- 建立运营身份（User.role=OPERATOR）与鉴权守卫，非运营访问管理端点返回 `OPERATOR_REQUIRED`（403）。
- 运营下架公开信息（→ `REMOVED`），`REMOVED` 信息不出现在公开搜索/详情。
- 运营限制账户（→ `RESTRICTED`，复用 abuse-prevention 的阻断）与解除限制（→ `ACTIVE`）。
- 运营分页查看全部举报与审计日志。
- 每个管理动作写入 `AuditEvent`（操作者、事件类型、目标、原因、时间）。
- 两后端等价实现。

**Non-Goals:**

- 不实现细粒度「禁言」（仅禁消息）与「封禁」（永久）——MVP 折叠为单一 `RESTRICTED` 状态，后续再拆分。
- 不实现独立 `ModerationAction` 实体——管理动作统一由 `AuditEvent` 记录（`event_type` 承载动作语义）。
- 不实现运营对举报的「处理结论」状态机（如举报 → 已处理），属后续 change。
- 不实现运营后台（admin-web）前端，本 change 仅后端契约与实现。
- 不实现运营审计的按目标/时间过滤（MVP 仅全量分页）。

## Decisions

1. **运营身份**：运营是 `User.role = OPERATOR` 的平台用户（默认 `USER`）。`User` 增加 `role` 字段；JWT subject 仍为 userId，角色在服务端从用户记录读取（不写入 JWT、不暴露在 `/me`）。mock 登录新增 `operator-code → openid_operator_001`，该 openid 登录即 `OPERATOR`。生产环境由运营账号目录（配置/DB）决定，本 change 用确定性 mock openid。

2. **鉴权守卫**：新增 `OperatorGuard`（Java）/ `require_operator`（Python）依赖，校验调用者 `role == OPERATOR`，否则抛 `OPERATOR_REQUIRED`（403）。所有 `/api/v1/admin/*` 端点先过该守卫，再执行动作。

3. **端点命名与形态**：运营端点统一在 `/api/v1/admin/` 命名空间下，共 5 个：
   - `POST /admin/posts/{postId}/remove`（body `{reason}`）→ `ItemPostView`（`REMOVED`）。
   - `POST /admin/users/{userId}/restrict`（body `{reason}`）→ `UserStatusView`（`RESTRICTED`）。
   - `POST /admin/users/{userId}/unrestrict`（body `{reason}`）→ `UserStatusView`（`ACTIVE`）。
   - `GET /admin/reports` → 全部举报分页列表（复用 `ReportView`/`ReportListData`）。
   - `GET /admin/audit-events` → 审计日志分页列表。

4. **下架状态机**：`PUBLISHED`/`VERIFYING`/`PENDING_HANDOVER` → `REMOVED`（对应 §9.1「任意公开状态 → 违规处理成立」）；其他状态（含 `REMOVED`、`CLOSED`、`COMPLETED`、`DRAFT` 等）返回 `INVALID_STATE_TRANSITION`（409）。下架后该信息自动从公开搜索/详情消失（`listVisible`/`isPublic` 白名单已排除 `REMOVED`），被收藏者在其收藏列表中看到 `status=REMOVED`（标注「已下架」）。

5. **限制/解除为幂等 set-status**：`restrict` 置 `RESTRICTED`、`unrestrict` 置 `ACTIVE`，二者为幂等赋值（非状态机），用户存在即返回 200 并写审计事件；已处目标状态时重复操作仍返回 200（状态不变）。

6. **审计模型**：统一 `AuditEvent`（id、actor_id、event_type、target_type、target_id、metadata、created_at），`metadata` 记录 `{reason}`。事件类型：`POST_REMOVED`、`USER_RESTRICTED`、`USER_UNRESTRICTED`；目标类型：`POST`、`USER`。`ModerationAction` 不单独建（其语义并入 `AuditEvent.event_type`）。审计日志仅运营可见。

7. **reason 校验**：管理动作的 `reason` 必填、非空、≤200 字符，否则 `VALIDATION_FAILED`（422）。

8. **错误码**：新增 `OPERATOR_REQUIRED`（403）。下架非法状态用既有 `INVALID_STATE_TRANSITION`；目标不存在用 `NOT_FOUND`。

9. **契约测试**：mock 用户 `valid-code`（发布者）、`valid-code-2`（普通用户）、`operator-code`（运营）；探针覆盖：非运营访问 403、下架成功与搜索消失、下架非法状态 409、限制账户后触发 `ACCOUNT_RESTRICTED`、解除限制恢复、举报列表、审计日志数量与内容、空 reason 422。

## Risks / Trade-offs

- [单一 RESTRICTED 状态] 禁言/封禁折叠为 `RESTRICTED`，粒度较粗；后续按需拆分状态值。
- [运营身份仅 mock 映射] 生产需替换为真实运营目录与强认证（MFA/SSO），本 change 仅确定性 mock。
- [审计无过滤] MVP 仅全量分页，运营后台检索需后续 change。
- [无持久化] 审计与运营目录内存存储，重启即失；随持久化 change 落地。
- [下架不可逆] REMOVED 无恢复端点；恢复申诉属后续 change。

## Migration Plan

- 无既有数据迁移（内存存储）。`User.role` 为新增默认字段（既有用户默认 `USER`）。新增 1 个错误码、5 个端点与 7 个 schema 为契约增量。
