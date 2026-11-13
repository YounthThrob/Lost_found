# Tasks

## 1. 契约落地

- [x] 1.1 在 `openapi/product-api.yaml` 新增 `POST /me/campus-verification/email`、`POST /me/campus-verification/email/confirm`、`POST /posts/{postId}/images`（multipart）与 `DELETE /posts/{postId}/images/{imageId}`；扩展 `CampusProfileResponse`（`verificationStatus` 增 `EMAIL_VERIFIED`、新增 `verifiedEmail`/`emailVerifiedAt`）与 `ItemPostView`（新增 `images`），验证 OpenAPI 校验通过
- [x] 1.2 在 `contracts/error-codes.yaml` 新增 `VERIFICATION_CODE_INVALID`（422，验证码错误或过期）

## 2. Java 实现

- [x] 2.1 `CampusProfile` 增加 `verificationStatus`/`verifiedEmail`/`emailVerifiedAt`；`InMemoryUserStore` 增加核验状态读写，验证既有账户测试不回归
- [x] 2.2 新增 `VerificationProvider` 接口与 `EmailProvider`（mock 固定验证码 `123456`、有效期 300s、`CAMPUS_EMAIL_DOMAINS` 域名白名单），验证 provider 单测通过
- [x] 2.3 `AccountController` 新增发起/确认两个端点，校验邮箱格式与验证码；集成测试覆盖发送成功、格式非法 422、域名不允许 422、核验成功置 `EMAIL_VERIFIED`、验证码错误返回 `VERIFICATION_CODE_INVALID`、重新核验覆盖
- [x] 2.4 新增 `Storage` 接口与 `LocalFileStorage`（写 `./data/media`、key 仅含 `[0-9a-f-]` 防路径穿越），验证 storage 单测通过
- [x] 2.5 `ItemImage` 模型与存储；`POST /posts/{postId}/images`（类型/大小/数量校验）与 `DELETE /posts/{postId}/images/{imageId}`；集成测试覆盖上传成功、类型/大小/数量非法 422、非作者 404、非草稿 409、删除成功
- [x] 2.6 `ItemPostView` 增加 `images`（公开附全部、草稿仅作者）并配置 `/media/**` 静态托管；集成测试覆盖公开详情与搜索附图片、草稿仅作者可见、非作者查草稿 404

## 3. Python 实现

- [x] 3.1 `CampusProfile` 增加 `verification_status`/`verified_email`/`email_verified_at`；store 增加核验状态读写，验证既有账户测试不回归
- [x] 3.2 新增 `VerificationProvider` 协议与 `EmailProvider`（mock 固定验证码 `123456`、有效期 300s、`CAMPUS_EMAIL_DOMAINS` 域名白名单），验证 provider pytest 通过
- [x] 3.3 router 新增发起/确认两个端点，校验邮箱格式与验证码；pytest 覆盖发送成功、格式非法 422、域名不允许 422、核验成功、验证码错误、重新核验覆盖
- [x] 3.4 新增 `Storage` 协议与 `LocalFileStorage`（写 `./data/media`、key 校验），验证 storage pytest 通过
- [x] 3.5 `ItemImage` 模型与存储；上传与删除端点（类型/大小/数量校验）；pytest 覆盖上传成功、校验失败 422、非作者 404、非草稿 409、删除成功
- [x] 3.6 `ItemPostView` 增加 `images` 并挂载 `/media/**` 静态托管；pytest 覆盖公开可见性与草稿仅作者可见

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：校园邮箱核验全链路（发起→确认→状态查询）与图片上传校验用例
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证 `java == python == expected`

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change（`/opsx:archive`），将 delta 合并进 `openspec/specs/` 对应 capability。
