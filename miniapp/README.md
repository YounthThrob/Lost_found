# 校园失物招领小程序（非官方）· 客户端

原生微信小程序（WXML / WXSS / TypeScript），消费 `openapi/product-api.yaml` 定义的契约。

## 目录结构

- `app.json` / `app.ts` / `app.wxss`：全局配置与启动逻辑。
- `config.ts`：后端地址与开发环境 mock 登录 code。
- `types/api.ts`：与 OpenAPI schema 一一对应的类型。
- `utils/`：请求封装、登录、格式化、状态文案。
- `pages/`：16 个页面，覆盖 PRODUCT.md §6 页面清单（首次进入 / 首页广场 / 发布 / 详情 / 认领申请 / 申请管理 / 我的申请 / 沟通 / 交接 / 我的 / 我的发布 / 收藏 / 学校校区 / 黑名单 / 举报 / 设置）。

## 运行

1. 用微信开发者工具「导入项目」，选择本 `miniapp/` 目录；AppID 可填测试号（`project.config.json` 已置 `touristappid`）。
2. 开发者工具勾选「不校验合法域名」（本地联调时请求 `http://127.0.0.1`）。
3. 确认 `config.ts` 的 `API_BASE` 指向已启动的后端（Java 默认 8080，Python 默认 8000）。
4. 开发环境未接微信登录时，`config.ts` 的 `MOCK_CODE` 使用后端 mock code（`valid-code` 等）；上线前置空改走 `wx.login`。

## 类型检查

```bash
npm install
node node_modules/typescript/bin/tsc --noEmit
```

> 注意：本仓库路径含空格（`E:\develop project\...`），Windows 下 `npx tsc` / npm `.bin` shim 会解析失败，需用 `node node_modules/typescript/bin/tsc` 直接调用。
