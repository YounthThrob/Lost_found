# 校园失物招领（非官方）· 运营管理后台

React 18 + Vite + TypeScript + Ant Design，消费 `openapi/product-api.yaml` 的运营接口。

## 页面

- 举报处理：查看全部举报，对举报的信息执行下架。
- 用户与信息处理：按 ID 下架信息、限制 / 解除账户。
- 审计日志：查看运营操作记录（下架 / 限制 / 解除）。

## 运行

1. `npm install`
2. 确认 `src/config.ts` 的 `API_BASE` 指向已启动的后端（Java 默认 8080，Python 默认 8000）。
3. 登录：使用运营账号密码（默认 `admin` / `admin123`，生产通过后端环境变量 `OPERATOR_USERNAME` / `OPERATOR_PASSWORD` 覆盖）。
4. 启动开发服务器或构建：

```bash
npm run dev        # 开发（http://localhost:5173）
npm run build      # 生产构建
```

> 注意：本仓库路径含空格（`E:\develop project\...`），Windows 下 npm 的 `.bin` shim 会解析失败。若 `npm run build` 报 `Cannot find module ...\typescript\bin\tsc`，改用：
>
> ```bash
> node node_modules/typescript/bin/tsc -b
> node node_modules/vite/bin/vite.js build
> ```
