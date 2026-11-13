# Tasks: user-profile

- [x] 契约：`openapi/product-api.yaml` 扩展 `MeResponse`（nickname/avatarUrl），新增 `ProfileUpdate`、`ProfileResponse`、`AvatarResponse` 与 `/me/profile`（PUT）、`/me/avatar`（POST）端点（无新错误码，复用 VALIDATION_FAILED）。
- [x] 契约测试：`contracts/run_contract_tests.py` 新增 profile 向量（设置/空/超长昵称、上传头像、非法类型、超大、`GET /me` 反映、公开详情不泄露昵称头像）。
- [x] Python：`UserAccount`/`InMemoryUserStore` 增加 nickname/avatarKey 与 update_profile，`account/service.py` 增加 update_profile/set_avatar 校验与存储，`account/router.py` 增加 `/me/profile`、`/me/avatar`，`GET /me` 返回 nickname/avatarUrl。
- [x] Java：`UserAccount`/`InMemoryUserStore`/`AccountService`/`AccountController` 等价实现（含 Storage 注入与头像上传）。
- [x] 小程序：`MeView` 增加 nickname/avatarUrl，新增 `pages/profile/profile` 资料编辑页（chooseAvatar + nickname），「我的」页展示昵称/头像并接入编辑页。
- [x] 契约测试运行：java == python == expected（`mvn -DskipTests package` 后跑 `contracts/run_contract_tests.py`）。
