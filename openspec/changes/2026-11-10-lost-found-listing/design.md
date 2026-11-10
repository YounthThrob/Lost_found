# Design

## Context

- 已有 `user-account`（微信登录 + 协议确认）与 `self-declared-campus`（自报学校与校区）。
- 协议门控已就绪：Java 的 `@RequiresAgreement` 注解 + `AgreementInterceptor`（全局注册）+ `AgreementGuard`，Python 的 `require_agreement` 依赖。
- PRODUCT.md §9.1 定义信息状态机；§11.2 定义分页格式与核心错误码；REQ-002 发布信息、REQ-003 搜索与筛选。
- 当前安全模型：除 `/session/wechat` 外所有接口均要求有效会话（Java `anyRequest().authenticated()`，Python `get_current_user`）。

## Goals / Non-Goals

**Goals:**

- 建立 `ItemPost` 数据模型与内存存储，落地 `DRAFT/PENDING_REVIEW/PUBLISHED/CLOSED` 状态机，迁移由服务端校验。
- 提供创建草稿、编辑草稿、发布、关闭、详情、搜索六类接口。
- 落地分页契约（`items` + `pagination`）与 `NOT_FOUND`、`INVALID_STATE_TRANSITION` 错误码。
- 确保公开搜索与详情只暴露已发布信息，绝不泄露私密核验特征（本 change 的 ItemPost 不含 PrivateClue 字段）。

**Non-Goals:**

- 不实现图片上传与对象存储（另立 change；本 change 的 ItemPost 不含图片字段）。
- 不实现招领的私密核验特征 PrivateClue（属 `claim-verification`，另立 change）。
- 不实现真实内容审核（自动通过审核为 MVP 占位；`content-moderation` 后续接入 PENDING_REVIEW → PUBLISHED/REJECTED 的真实判定）。
- 不实现 `occurredFrom/occurredTo`（大致时间）与 `EXPIRED/REMOVED/VERIFYING/PENDING_HANDOVER/COMPLETED` 等后续状态。
- 不实现相关度排序与校区/分类下拉数据源。

## Decisions

1. **状态机（MVP 子集）**：`DRAFT → PENDING_REVIEW → PUBLISHED → CLOSED`。`publish` 先校验 DRAFT 迁移到 PENDING_REVIEW，随即自动通过审核迁移到 PUBLISHED；`close` 校验 PUBLISHED 迁移到 CLOSED。理由：保留契约状态机的完整轨迹，`content-moderation` 后续只需替换「自动通过」为真实判定，无需改客户端。非法迁移返回 `INVALID_STATE_TRANSITION`（409）。

2. **数据模型**：`ItemPost` 为不可变 record/dataclass，字段 `id/authorId/type/category/title/publicDescription/school/campus/area/status/createdAt/updatedAt`。`campus`、`area` 可空。不落 `version`（乐观锁留待交接/认领并发时引入）。内存存储，`id` 用 UUID。

3. **可见性**：搜索仅返回 `PUBLISHED`；详情对 `PUBLISHED` 所有人可见、非 `PUBLISHED` 仅作者可见、否则 `NOT_FOUND`。公开接口不返回 openid（仅 `authorId` UUID），不返回任何私密字段。

4. **分页与排序**：`GET /posts` 查询参数 `page`（1 起）/`pageSize`（默认 20 上限 100）/`type`/`category`/`school`/`campus`/`keyword`；响应 `data.items` + `data.pagination{page,pageSize,total,hasMore}`。排序默认按 `createdAt` 倒序。`keyword` 对标题与公开描述做大小写不敏感包含匹配。

5. **鉴权与门控**：所有 listing 接口要求有效会话；写接口（create/update/publish/close）额外要求已确认协议。读接口（search/detail）不要求协议确认。复用现有会话校验与协议门控，不改 SecurityConfig。

6. **编辑语义**：`PATCH /posts/{postId}` 对草稿做全量替换可编辑字段（type/category/title/publicDescription/school 必填，campus/area 可选），仅 `DRAFT` 可编辑。避免 null 与「未提供」的歧义。

7. **校验一致性**：字段长度与必填校验在 service 层完成，抛 `VALIDATION_FAILED`（422）；`type` 非 `LOST/FOUND` 亦为 VALIDATION_FAILED。两后端对同一非法输入返回一致。

8. **微信 mock 扩展**：新增 `valid-code-2 → openid_test_002`，用于多用户可见性测试。仅影响 dev/test mock，不影响契约。

## Risks / Trade-offs

- [状态机与后续能力衔接] → 本 change 只实现 MVP 子集，`PENDING_REVIEW → PUBLISHED` 自动通过；REJECTED/EXPIRED/REMOVED 等留待 `content-moderation` 与生命周期能力扩展。契约测试仅覆盖 MVP 迁移。
- [不可变 record 字段多] → create/update 均重建完整对象；edit 保留 `createdAt`、仅更新 `updatedAt`。
- [搜索性能] → MVP 内存线性扫描，数据量小可接受；SQLAlchemy/JPA 与索引留待持久化 change。

## Migration Plan

- 无既有数据迁移（内存存储）。ItemPost 默认无既有记录。
