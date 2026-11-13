# Design

## Context

两个后端均为内存 walking skeleton，契约由 `openapi/product-api.yaml`、`contracts/error-codes.yaml` 与 `contracts/run_contract_tests.py` 共同约束（Java Spring Boot 3 / Python FastAPI，行为等价）。现状：`self-declared-campus` 只返回 `verificationStatus: SELF_DECLARED`；`lost-found-listing` 的信息视图无图片字段。动机见 proposal.md。

## Goals / Non-Goals

**Goals:**

- 校园邮箱核验（mock、固定验证码）+ 可替换的身份验证抽象，为后续接入微信「学生验证」留接口位。
- 丢失物品图片上传/删除，本地文件系统 + 存储抽象落地，数据库只存 `storage_key`。
- 两个后端对同一请求保持完全等价的契约行为。

**Non-Goals:**

- 不接真实 SMTP 发信、不做真实图片审核（`moderation_status` 仅预留字段，MVP 置 `PENDING`）。
- 不接微信「学生验证」（仅留 Provider 位）。
- 不强制「至少一张图片」；不处理「修改自报学校后原邮箱核验失效」的联动。

## Decisions

### 1. 身份验证抽象（VerificationProvider）

提供方接口定义两个能力：`send(email) → { expiresInSeconds }` 与 `confirm(email, code) → bool`。MVP 实现 `EmailProvider`（mock）：固定验证码 `123456`，`send` 返回固定有效期 300 秒，任何格式合法的邮箱均可核验成功；后续 `WechatStudentProvider` 只需实现同一接口并替换注入。

- 替代：把邮箱逻辑直接写死在 service —— 否决，违背分阶段「可替换层」目标。

### 2. 校园邮箱域名白名单

配置 `CAMPUS_EMAIL_DOMAINS`（逗号分隔，默认空）。为空表示不限制（开发用）；非空时提取邮箱 `@` 后域名做精确匹配，不在白名单返回 `VALIDATION_FAILED`（422）。

- 替代：硬编码 `*.edu.cn` 正则 —— 否决，太宽泛且不可配置。

### 3. 图片存储抽象（Storage）

接口 `put(key, bytes, contentType)` / `get(key)` / `delete(key)`。MVP 实现 `LocalFileStorage`，写本地目录（如 `./data/media/{key}`），经静态托管对外提供。数据库只存 `storage_key`（UUIDv7 不可预测）。key 用「图片 id 或独立 UUID」生成，避免路径穿越（校验 key 只含 `[0-9a-f-]`）。

- 替代：直接对接 OSS/COS —— 否决（用户选定本地 + 抽象）。

### 4. 图片 URL 与静态托管

API 返回相对路径 `url = /media/{storage_key}`，客户端用后端 origin 拼接（miniapp 已有 `API_BASE` 配置可推导 origin）。`/media/**` 为二进制静态资源，不入 JSON 契约；Java 用 Spring 资源 handler、Python 用 `StaticFiles` 挂载同一语义路径。草稿图片不做接口鉴权，仅靠不可预测 key 隔离，且只在作者响应中下发 URL。

### 5. multipart 上传与校验

`POST /posts/{postId}/images` 用 `multipart/form-data`，文件字段名 `file`。服务端校验：类型 ∈ {jpeg,png,webp}、大小 ≤ 10MB、单帖 ≤ 9 张。常量 `MAX_IMAGE_BYTES = 10 * 1024 * 1024`、`MAX_IMAGES_PER_POST = 9`。所有校验失败统一 `VALIDATION_FAILED`（422），`details` 用 `{field, reason}` 区分 type/size/count。非作者 `NOT_FOUND`（404）、非草稿 `INVALID_STATE_TRANSITION`（409）。

### 6. 数据模型扩展

- `CampusProfile` 增加 `verification_status`（`SELF_DECLARED` | `EMAIL_VERIFIED`）、`verified_email`（可空）、`email_verified_at`（可空，RFC3339 UTC）。
- `ItemImage`：`id, post_id, storage_key, sort_order, moderation_status`（MVP `moderation_status` 置 `PENDING`）。
- `ItemPostView` 增加 `images: [{ id, url, sortOrder }]`。

### 7. 图片可见性实现

组装 `ItemPostView` 时：公开状态（`PUBLISHED`/`VERIFYING`）附全部图片；`DRAFT` 仅在请求者为作者时附图片；非作者查草稿返回 404。搜索仅返回公开信息并附图片（复用同一视图组装逻辑）。

### 8. 契约与错误码

新增 `VERIFICATION_CODE_INVALID`（422，验证码错误或过期）；图片校验复用 `VALIDATION_FAILED`（422）；状态冲突复用 `INVALID_STATE_TRANSITION`（409）；越权复用 `NOT_FOUND`（404）。

## Risks / Trade-offs

- [草稿图片 URL 无接口鉴权] → 靠 UUIDv7 不可预测 + 不进入日志/索引隔离；后续需强隔离再上鉴权或签名 URL。
- [mock 固定验证码 `123456` 可被任意核验] → 仅 dev/mock，上线前必须替换为真实邮件或微信学生验证。
- [本地文件系统不横向扩展] → 存储抽象已隔离，后续换对象存储无需改契约。
- [图片不加密] → 丢失物品图片属公开内容，不属于 PrivateClue/ClaimEvidence 隐私红线；前端仍需提示遮挡证件号、人像、二维码等敏感信息。
- [应用层校验 multipart 大小] → 前端 + 后端双重校验，超限即拒，避免落盘大文件。

## Migration Plan

无数据库迁移（in-memory walking skeleton）。新增字段默认 `SELF_DECLARED`/`null`，对存量数据透明。切换真实存储或真实验证提供方时仅替换实现，不改契约。

## Open Questions

- 校园邮箱域名白名单的首批学校（等试点学校确定后再配置）。
- 真实图片审核服务选型（第三方或人工）——MVP 不启用，仅留 `moderation_status` 字段。
