# Design

## Context

- 已有 `claim-verification`（认领提交）、`lost-found-listing`（发布）、`safe-communication`（消息）、`content-moderation`（举报与关键词检测）。
- PRODUCT.md §11.2 已列 `ACCOUNT_RESTRICTED`(403)、`RATE_LIMITED`(429)、`CLAIM_RATE_LIMITED`(429)，尚未实现。
- PRODUCT.md §13 要求对登录、发布、认领、消息和举报分别限流。
- PRODUCT.md REQ-001 要求账户处于限制状态时拒绝发布/留言/认领；REQ-004 要求认领频率超限时拒绝且不泄露风控细节。
- 账户守卫现状：`@RequiresAgreement`/`require_agreement` 门控发布、认领、消息与交接创建。

## Goals / Non-Goals

**Goals:**

- 对认领、发布、消息、举报四类操作按用户维度实施固定窗口频率限制，认领返回 `CLAIM_RATE_LIMITED`，其余返回 `RATE_LIMITED`。
- 对 `RESTRICTED` 账户拒绝发布/留言/认领，返回 `ACCOUNT_RESTRICTED`。
- 风控错误响应不泄露阈值、窗口或规则细节。

**Non-Goals:**

- 不实现审计日志 `AuditEvent`（当前无运营管理动作可记录，属后续 change）。
- 不实现登录限流（需 IP/设备维度，mock 环境不具备）。
- 不实现运营触发封禁/禁言的端点（admin API 属后续 change；本 change 仅提供强制执行守卫与状态存储）。
- 不实现滑窗/令牌桶等更精细限流算法，MVP 用固定窗口。
- 不引入额外反垃圾模型（`content-moderation` 关键词检测已覆盖 MVP 反垃圾）。

## Decisions

1. **限流算法**：固定窗口（60 秒），按 `userId` 维度、按操作独立计数。命中上限即抛限流错误码，且每次请求都计数（无论业务校验是否通过）。窗口与阈值见 `contracts/rate-limits.yaml`（claim=10、post=10、message=20、report=10 / 60s）。

2. **检查顺序**：鉴权 → 协议/账户限制守卫（`@RequiresAgreement`/`require_agreement`）→ 限流检查 → 业务校验。这样受限账户优先返回 `ACCOUNT_RESTRICTED`，正常账户再受限流约束，Java/Python 行为一致。

3. **注入点**：限流器作为独立组件注入四处——`ClaimService.submit`（claim）、`ListingService.createDraft`（post）、`CommunicationService.send`（message）、`ReportService.createReport`（report）。发布仅限流「创建信息」入口（`POST /posts`），publish/close 不再重复限流。

4. **账户限制**：`UserAccount.status` 取值 `ACTIVE`/`RESTRICTED`。将限制校验并入既有守卫 `requireAgreed`/`require_agreement`（先协议、后限制），因此发布、认领、消息、交接创建同时受账户限制约束。触发限制的 `restrict`/`unrestrict` 仅作为存储方法暴露（供后续 admin 动作与测试使用），本 change 不新增对外端点。

5. **配置落地**：限流阈值作为常量硬编码进 Java/Python（注释指向 `contracts/rate-limits.yaml`），与 `content-moderation` 的违禁词一致，保持契约优先且两实现等价。

6. **契约测试**：`valid-code-4` 作为全新用户用于限流探针；限流检查位于业务校验之前，探针可用固定 postId 反复请求，第 N+1 次触发限流码，无需构造大量真实业务对象。账户限制的触发端点在契约测试中不可达，改由两后端单元测试直接操作存储验证。

## Risks / Trade-offs

- [固定窗口边界] 窗口为 60 秒，跨边界会重置计数；测试均在毫秒级完成，确定性不受影响。
- [限流检查前置] 限流先于业务校验，即使请求最终会被业务拒绝也计入限流；属更防御性的反滥用语义，已写入 spec。
- [账户限制无触发器] `RESTRICTED` 仅守卫与存储，暂无运营端点触发，避免引入 admin 鉴权面；后续 change 补齐。
- [审计延后] 无管理动作即无审计对象，审计日志随运营能力落地。
- [无持久化] 限流计数内存存储，重启即失；随持久化 change 落地。

## Migration Plan

- 无既有数据迁移（内存存储）。新增 3 个错误码、`contracts/rate-limits.yaml` 与限流器为契约增量。
