# Tasks

- [x] 契约：openapi 三个只读操作 security 改为可选鉴权并移除 401
- [x] Python：新增 get_optional_user，只读端点改用之，get_detail 接受空 viewer
- [x] Java：SecurityConfig 放行三个 GET，controller 处理可选认证，getDetail 空安全
- [x] 契约测试：新增公开访问/写操作鉴权/无效令牌向量
- [x] 小程序：app.ts 启动进入首页，request.ts 401 改为登录提示
- [x] 小程序：新增 ensureLoggedIn，写入口与「我的」页接登录门
- [x] 小程序：登录页协议勾选 + 可点击全文 + 协议全文页
