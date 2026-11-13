# Proposal: user-profile（微信昵称与头像授权）

## 为什么

用户希望实现「真实的微信授权登录」。其中 `wx.login` → `code2session` → `openid` 的登录链路在两个后端均已实现（受 `WECHAT_MOCK` 开关控制，当前无 AppSecret 时保留 mock 登录）。本次补齐微信授权登录的另一半：**昵称与头像授权**。

现代微信授权（2022+）已废弃 `wx.getUserProfile`，改为 `<button open-type="chooseAvatar">` 获取头像 + `<input type="nickname">` 获取昵称，由用户主动选择后由客户端上传/保存到平台。

## 改什么

- 账户模型新增 `nickname`（昵称）与 `avatarKey`（头像存储键）两个字段。
- 新增两个受登录保护的端点：
  - `PUT /me/profile`：保存昵称。
  - `POST /me/avatar`：上传头像（multipart）。
- `GET /me` 扩展返回 `nickname` 与 `avatarUrl`。
- 小程序新增「个人资料」编辑页（chooseAvatar + nickname 输入），并在「我的」页展示昵称与头像。

## 硬约束遵守

- **契约优先**：先改 `openapi/product-api.yaml` 与契约测试向量，再改实现。
- **双后端等价**：Java/Python 对同一请求返回一致的 HTTP 状态码、业务错误码、字段名。
- **隐私红线**：昵称与头像 SHALL NOT 进入公开接口（列表/详情/搜索）、普通日志、埋点、搜索索引或分析系统，仅通过 `GET /me` 返回给本人。
- **非官方边界**：昵称/头像为用户或微信提供，不构成任何身份、校方或官方认证，文案不得暗示 verified/certified/official。
- **密钥不进版本库**：头像存储复用现有本地文件存储，不引入新密钥；微信 AppSecret 仍未提供，登录继续走 mock。

## 范围外

- 真实的 `code2session` 调用（已有实现，仅待 AppSecret 配置后开启）。
- 头像图片内容审核（moderation）与第三方对象存储。
