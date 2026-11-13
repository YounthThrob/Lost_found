# Proposal

## Why

两个能力补齐 MVP 的关键闭环：其一，「校园身份」目前只有自报学校、缺少任何可信度信号，需要一种仍守住「非官方」边界的轻量校园核验方式；其二，失物招领信息目前只有文字、无法上传物品图片，直接影响检索与核验体验。二者均已出现在 PRODUCT.md 的数据模型与需求中（`ItemImage` 实体、REQ-002「至少一张图片」），本次落地。

## What Changes

- 新增 `campus-verification` capability：校园邮箱核验（MVP 用 mock 邮件、固定验证码），核验成功后 `verificationStatus` 由 `SELF_DECLARED` 提升为 `EMAIL_VERIFIED`，仍为非官方、非校方认证。
- 身份验证抽象为可替换层（VerificationProvider 接口），MVP 实现 EmailProvider，预留后续接入微信「学生验证」Provider。
- 新增 `item-images` capability：为寻物/招领信息上传多张图片（上限 9 张，jpeg/png/webp，单张 ≤10MB），MVP 用本地文件系统 + 存储抽象，数据库只存 `storage_key`。
- 修改 `self-declared-campus`：`verificationStatus` 允许 `SELF_DECLARED | EMAIL_VERIFIED`，仍 SHALL NOT 出现 `verified`/`certified`/`official` 等认证语义。
- 修改 `lost-found-listing`：`ItemPostView` 增加 `images` 列表，详情/搜索/我的发布随附图片；草稿图片仅作者可见。
- 契约优先：同步修改 `openapi/product-api.yaml`、`contracts/error-codes.yaml`（新增 `VERIFICATION_CODE_INVALID`）与契约测试向量。无破坏性变更。

## Capabilities

### New Capabilities

- `campus-verification`: 校园邮箱核验流程（发起、确认、查询状态）与可替换的身份验证抽象，始终为非官方、非校方认证。
- `item-images`: 丢失物品图片的上传、删除、校验、存储抽象与可见性规则。

### Modified Capabilities

- `self-declared-campus`: 「自报学校标记为非认证」要求扩展为支持 `EMAIL_VERIFIED` 状态，仍禁止任何官方/可信/认证语义。
- `lost-found-listing`: 信息视图（详情与搜索）增加图片列表，公开信息附公开图片、草稿图片仅作者可见。

## Impact

- 修改 `openapi/product-api.yaml`：新增 `/me/campus-verification/email`、`/me/campus-verification/email/confirm`、`/posts/{postId}/images`（multipart 上传）、`/posts/{postId}/images/{imageId}`；扩展 `CampusProfileResponse`（`verificationStatus` 增加 `EMAIL_VERIFIED`、新增 `verifiedEmail`）与 `ItemPostView`（新增 `images`）。
- 修改 `contracts/error-codes.yaml`：新增 `VERIFICATION_CODE_INVALID`（422）。
- 修改 `backend-java/` 与 `backend-python/`：`CampusProfile` 模型、`VerificationProvider` 抽象与 EmailProvider（mock）、`Storage` 抽象与本地文件系统实现、multipart 上传、静态图片托管（`/media/{storageKey}`）、集成与契约测试。
- 新增配置：`CAMPUS_EMAIL_DOMAINS`（可选域名白名单，为空表示不限制）、图片存储目录、单张大小与单帖数量上限常量。
- 图片托管路径 `/media/{storageKey}` 为二进制静态资源，不入 JSON 契约，URL 格式在 design.md 统一约定。
