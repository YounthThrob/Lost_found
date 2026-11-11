# Design

## Context

- 已有 `lost-found-listing`（信息发布/搜索，`PUBLISHED`/`VERIFYING` 为公开可见状态）、`content-moderation`（违禁词检测）、`abuse-prevention`（固定窗口限流 + 账户限制守卫）、`safe-communication`（有限消息）。
- PRODUCT.md §11 已列线索 3 端点与 `Clue` 数据模型，尚未实现；§9.3 定义线索状态机 `SUBMITTED → ACCEPTED → CLOSED`（终态 `REJECTED`、`EXPIRED`）。
- PRODUCT.md REQ-009：线索仅对寻物（LOST）信息提交、仅失主及运营可见、失主可查看/标记有效/标记无效、提交须过内容审核与频率限制。

## Goals / Non-Goals

**Goals:**

- 对「展示中」的 LOST 信息提交线索，校验类型与可见性，返回 `SUBMITTED`。
- 线索内容过违禁词检测，命中保存为 `REJECTED` 且不出现在失主待处理列表。
- 失主分页查看待处理线索；非失主不可见（`NOT_FOUND`）。
- 失主标记线索有效（→ `ACCEPTED`）或无效（→ `CLOSED`），状态迁移服务端校验。
- 线索提交受频率限制（`RATE_LIMITED`）；线索内容不进入任何公开接口。

**Non-Goals:**

- 不实现线索可选图片（`storage_key`），MVP 为纯文本，随对象存储 change 落地。
- 不实现「有效线索进入有限沟通」（复用 REQ-006）的桥接，属后续 change。
- 不实现运营人员查看线索（运营身份/鉴权尚未建立）。
- 不实现「线索转人工审核」队列，MVP 命中违禁词即 `REJECTED`（与信息发布一致）。
- 不实现 `EXPIRED` 联动（寻物信息关闭/过期时联动关闭线索），属后续 change。

## Decisions

1. **端点与语义**：`POST /posts/{postId}/clues` 提交（body `{content}`）；`GET /posts/{postId}/clues` 失主分页查看待处理线索；`POST /clues/{clueId}/decide` 判定（body `{decision: VALID|INVALID}`）。`VALID` → `ACCEPTED`，`INVALID` → `CLOSED`（对应 REQ-009「标记有效/标记无效」；§9.3 无独立 CLOSE 态，故「关闭」并入「标记无效」）。

2. **可提交性校验顺序**：鉴权 → 协议守卫（`@RequiresAgreement`/`require_agreement`）→ 限流（clue）→ 信息存在性/可见性 → 类型（LOST）→ 内容校验 → 违禁词检测。信息不存在或不可见返回 `NOT_FOUND`（404），存在但为 FOUND 返回 `CLUE_NOT_ALLOWED`（409），与认领的 `POST_NOT_CLAIMABLE` 对称。

3. **内容审核复用**：复用 `content-moderation` 的违禁词检测（关键词列表与命中判定），命中即保存 `REJECTED`，未命中保存 `SUBMITTED`。线索 `content` 校验非空、≤2000 字符，超限 `VALIDATION_FAILED`（422）。

4. **可见性边界**：线索仅存在于失主可见的 `GET /posts/{postId}/clues` 响应中；不加入任何公开搜索/详情返回，`content` 不进日志。非失主访问线索列表/判定一律 `NOT_FOUND`（404），不泄露线索存在性。

5. **限流接入**：`contracts/rate-limits.yaml` 增加 `clue`，默认阈值与举报一致（20/60s）；提交线索超限返回 `RATE_LIMITED`（429）。复用 `abuse-prevention` 的限流器，按 userId 独立计数。

6. **状态机**：`Clue.status` ∈ `SUBMITTED | ACCEPTED | CLOSED | REJECTED | EXPIRED`。仅 `SUBMITTED` 可被 `decide` 迁移；其他状态重复判定返回 `INVALID_STATE_TRANSITION`（409）。`EXPIRED` 预留枚举值，联动逻辑后续实现。

7. **契约测试**：复用现有 mock 用户（`valid-code` 为失主，`valid-code-2` 为线索提供者）；探针覆盖：LOST 提交成功、FOUND 拒绝、非展示中拒绝、违禁词 `REJECTED`、失主查看/非失主 404、判定状态机、重复判定 409、限流。

## Risks / Trade-offs

- [判定语义折叠] 「关闭」并入「标记无效」（均 → CLOSED）；若后续需要区分，再拆独立动作。
- [违禁词命中断联] `REJECTED` 线索不再出现在待处理列表，失主不可见；属隐私优先的审核语义，与信息发布一致。
- [EXPIRED 未联动] 寻物信息关闭后线索仍保留 `SUBMITTED/ACCEPTED`；后续 change 补联动。
- [无图片] 线索纯文本，图片后续随对象存储落地。
- [无持久化] 线索内存存储，重启即失；随持久化 change 落地。

## Migration Plan

- 无既有数据迁移（内存存储）。新增 1 个错误码、`clue` 限流阈值、3 个端点与 6 个 schema 为契约增量。
