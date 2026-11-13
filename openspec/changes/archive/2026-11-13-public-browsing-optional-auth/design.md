# 设计：公开浏览与可选登录（public-browsing-optional-auth）

## 契约

对三个只读操作的 `security` 改为「可选鉴权」惯用写法（先允许匿名、再允许 bearer）：

```yaml
security:
  - {}
  - bearerAuth: []
```

并从这三个操作移除 401 响应（公开端点不再返回 401）。写操作端点保持不变。

## Python

- 在 `app/security/deps.py` 新增 `get_optional_user`：复用 `HTTPBearer(auto_error=False)`，无凭证或解析失败/用户不存在时返回 `None`（不抛 `SESSION_EXPIRED`）。
- `listing/router.py` 的 `get_post`、`search_posts`、`get_stats` 改用 `get_optional_user`；`search_posts`、`get_stats` 本就不使用 user_id，直接去掉该依赖。
- `listing/service.py::get_detail` 的 `viewer_id` 改为 `str | None`；`None` 时非公开状态仍返回 NOT_FOUND（`post.author_id != None` 恒成立，行为正确）。

## Java

- `SecurityConfig`：在 `anyRequest().authenticated()` 之前新增
  `.requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/{postId}", "/api/v1/stats").permitAll()`。
- `ListingController`：`getPost` 改为 `viewerId = authentication == null ? null : authentication.getName()` 后调用 `getDetail`；`searchPosts`、`stats` 移除未使用的 `Authentication` 参数。
- `ListingService.getDetail` 的 `viewerId` 允许为 null：`post.authorId().equals(null)` 返回 false，行为正确，无需额外空判断。

## 契约测试

`contracts/run_contract_tests.py` 新增向量：

- `GET /posts`、`GET /posts/{postId}`（公开）、`GET /stats` 无 token 返回 200。
- `POST /posts` 无 token 返回 401 `SESSION_EXPIRED`。
- 公开只读端点携带无效 token 仍返回 200。

断言 Java == Python == expected。

## 小程序

- `app.ts`：启动不再强制跳登录；默认入口为首页 tab（`pages/index/index`）。
- `utils/request.ts`：401 不再盲目 `reLaunch`；改为清除 token 并统一提示「请先登录」，仅写操作路径受影响（公开端点不再 401）。
- `utils/auth.ts`：新增 `ensureLoggedIn()`：已登录返回 true；否则弹「去登录」提示并跳转 `pages/welcome/welcome`，返回 false。发布、详情页的认领/线索/收藏、`我的` 页等写入口先调用它。
- `pages/welcome`：协议改为「勾选 + 可点击查看全文」，未勾选时禁用进入按钮；新增协议全文页（用户协议 / 隐私说明），文案遵循非官方边界与隐私红线。

## 隐私与非官方边界

公开响应只含公开字段；协议文本明确「非官方、用户自报未经校方认证、请勿暴露敏感信息」。
