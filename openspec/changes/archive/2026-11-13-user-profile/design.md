# Design: user-profile

## 数据模型

- `UserAccount`（Java record / Python dataclass）新增两个可空字段：
  - `nickname: String | None`
  - `avatarKey: String | None`（数据库/内存仅存存储键，二进制交 Storage 抽象落地）

## 端点契约

| 方法 | 路径 | 请求 | 响应 data |
| --- | --- | --- | --- |
| GET | `/me` | —（登录保护） | `{ id, status, agreementAccepted, nickname, avatarUrl }` |
| PUT | `/me/profile` | `{ nickname }` | `{ nickname, avatarUrl }` |
| POST | `/me/avatar` | multipart `file` | `{ avatarUrl }` |

- `avatarUrl`：未设置头像时为 `null`；已设置时为 `/media/{avatarKey}`。
- 所有端点均要求有效会话（`bearerAuth`），沿用 `SESSION_EXPIRED`（401）。

## 校验

- 昵称：`strip` 后非空，长度 ≤ 30；否则 `VALIDATION_FAILED`（422）。
- 头像：`content_type ∈ {image/jpeg, image/png, image/webp}`，`len(content) ≤ 10MB`；否则 `VALIDATION_FAILED`（422）。复用物品图片的常量（`ALLOWED_IMAGE_TYPES` / `MAX_IMAGE_BYTES`）。
- 不新增业务错误码，复用 `VALIDATION_FAILED`。

## 存储

- 复用现有 `Storage` 抽象：Java `LocalFileStorage`、Python `LocalFileStorage`（`/media/**` 静态托管已存在）。
- 头像 key 用 `UUID.randomUUID().toString()` / `uuid4()`，满足 `[0-9a-f-]` 键约束，不可预测。
- 重新上传时删除旧 key（`storage.delete`），避免遗留孤儿文件；删除失败不影响主流程。

## 双后端等价

- Java `AccountController` / `AccountService` 与 Python `account/router.py` / `service.py` 一一对应。
- Java 头像上传用 `@RequestParam("file") MultipartFile` + `throws IOException`，与 `ListingController.uploadImage` 一致；Python 用 `UploadFile = File(...)`。
- 昵称/头像字段名、`avatarUrl` 前缀、错误码与 HTTP 状态码两端一致。

## 隐私与非官方

- 昵称/头像只写入 `UserAccount` 内存/模型，只由 `GET /me`、`PUT /me/profile`、`POST /me/avatar` 的响应返回；`ItemPostView` / `ItemPostListView` 不含这两个字段，天然不进入公开列表/详情/搜索。
- 不记录到普通日志、埋点、搜索索引、缓存或分析系统。
- 文案使用「昵称」「头像」等中性词，不出现 verified/certified/official；登录继续 mock（无 AppSecret）。

## 小程序

- 新增 `pages/profile/profile`：`<button open-type="chooseAvatar">` 选头像（`bind:chooseavatar` 拿到临时路径）→ `uploadFile` 上传；`<input type="nickname">` 编辑昵称 → `PUT /me/profile`。
- `GET /me` 类型 `MeView` 增加 `nickname`、`avatarUrl`；「我的」页展示头像与昵称，点击进入资料编辑页。
