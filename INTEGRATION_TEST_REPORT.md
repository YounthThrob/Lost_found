# 前后端联调问题记录

测试日期：2026-11-12（Asia/Shanghai）  
联调目标：微信小程序、运营后台与 Python 后端（`http://127.0.0.1:8000/api/v1`）  
记录原则：仅记录与复现，不修改产品代码。

## 测试结论

- Python 后端可正常启动。
- 登录 → 确认协议 → 创建草稿 → 发布 → 列表检索的真实 HTTP 链路通过。
- 微信小程序 TypeScript 类型检查通过。
- 运营后台 TypeScript 检查及 Vite 生产构建通过。
- Python 后端隔离环境测试通过：112 passed（含新增 CORS/会话回归 3 项）。
- 发现 2 个已确认的联调缺陷、2 个工程环境问题、1 个性能警告。
- 微信开发者工具窗口已被自动化工具准确识别，但画面采集连续超时，因此本轮没有完成模拟器内逐页点击验证。此项属于测试受阻，不能据此判断页面通过或失败。

## 已确认缺陷

### INT-001 运营后台请求被浏览器 CORS 拦截

- 严重度：高
- 状态：回归通过（2026-11-13）
- 影响范围：`admin-web` 连接 Python 后端；Java 后端代码也未见 CORS 配置，预计存在同类风险，但本轮因 Java 环境问题未完成黑盒确认。
- 前提：运营后台运行在 `http://localhost:5173`，后端运行在 `http://127.0.0.1:8000`。
- 复现步骤：
  1. 向 `OPTIONS /api/v1/session/wechat` 发送浏览器预检请求。
  2. 请求头包含 `Origin: http://localhost:5173`、`Access-Control-Request-Method: POST` 和 `Access-Control-Request-Headers: content-type`。
- 实际结果：HTTP 405；响应中没有 `Access-Control-Allow-Origin`。
- 预期结果：预检成功，并仅对配置的运营后台来源返回允许跨域的响应头。
- 证据：实测响应为 `Status=405`、`AllowOrigin=null`、`Body={"detail":"Method Not Allowed"}`。
- 定位：Python `app/main.py` 未注册 CORS 中间件；Java MVC/Security 配置中未发现 CORS 开启逻辑。
- 建议：两套后端均增加可由环境变量配置的来源白名单，不要在生产环境使用通配来源。
- 回归证据：当前基线提交 `729f18b`；真实预检请求返回 HTTP 200、响应体 `OK`，`Access-Control-Allow-Origin` 为 `http://localhost:5173`；自动化用例 `test_cors_preflight_allows_admin_origin` 与未知来源拒绝用例均通过。

### INT-002 后端重启后旧 token 导致 `/me` 返回 500

- 严重度：高
- 状态：回归通过（2026-11-13）
- 影响范围：微信小程序和运营后台的会话恢复；Python 已实测，Java 使用同类内存用户库和空值访问路径，存在同类代码风险，但本轮未完成 Java 黑盒确认。
- 前提：用户信息保存在内存中，JWT 密钥在后端重启前后不变，客户端本地仍保存旧 token。
- 复现步骤：
  1. 调用 `POST /api/v1/session/wechat` 获得有效 token。
  2. 清空/重建内存用户库，模拟后端重启。
  3. 携带旧 token 调用 `GET /api/v1/me`。
- 实际结果：HTTP 500，业务错误码 `INTERNAL_ERROR`。
- 预期结果：HTTP 401，业务错误码 `SESSION_EXPIRED`，使前端清理本地 token 并重新登录。
- 定位：JWT 依赖只验证签名与有效期，没有确认 token subject 对应用户仍存在；`/me` 随后直接访问空用户对象。
- 用户可见风险：小程序启动时检测到本地 token 就跳转首页；`fetchMe()` 的失败又被忽略，用户可能进入“看似登录、实际操作持续失败”的状态。
- 建议：认证层在建立会话上下文前校验用户是否存在，并统一返回 `SESSION_EXPIRED`；两套后端保持同样的 HTTP 状态和错误码。
- 回归证据：当前基线提交 `729f18b`；`tests/test_session_and_cors.py` 的旧 token 场景返回 HTTP 401 和 `SESSION_EXPIRED`。该文件连同两项 CORS 用例共 3 项测试通过。

## 工程与测试环境问题

### ENV-001 Python 测试命令容易误用全局环境

- 严重度：中（工程效率）
- 状态：已修复（2026-11-13）
- 现象：仓库根 README 未明确统一测试命令时，直接执行 `python -m pytest` 使用了 Anaconda 全局依赖，因旧版 Starlette 与新版 httpx 不兼容，13 个测试模块在收集阶段报错：`Client.__init__() got an unexpected keyword argument 'app'`。
- 额外现象：`uv run --extra test pytest -q` 在当前 Windows 环境中调用到错误的 pytest 入口，导致 `ModuleNotFoundError: No module named 'app'`。
- 可用命令：在 `backend-python` 目录运行 `uv run --extra test python -m pytest -q`。
- 验证结果：112 passed（含新增 CORS/会话回归 3 项）。
- 回归证据：已新增 backend-python/README.md 固化测试命令，避免依赖开发机全局 Python 包。
- 建议：把上述可用命令固化到 README 或项目脚本，并避免依赖开发机全局 Python 包。

### ENV-002 Java 测试无法启动

- 严重度：中（测试阻断）
- 状态：已修复（2026-11-13）
- 复现命令：在 `backend-java` 目录运行 `mvn test -q`。
- 实际结果：`JAVA_HOME environment variable is not defined correctly`。
- 影响：本轮无法验证 Java 后端单测、Java/Python 黑盒契约一致性，以及 INT-001/INT-002 在 Java 运行时的实际表现。
- 建议：将 `JAVA_HOME` 指向项目要求的 JDK 21 后重新执行 Java 单测和共享契约测试。
- 回归证据：已新增 backend-java/README.md 固化 JAVA_HOME 设置；Java 全量测试 115 passed，契约测试 java == python == expected。

## 警告与测试受阻项

### WARN-001 运营后台产物体积较大

- 严重度：低
- 状态：已缓解（2026-11-13）
- 现象：Vite 构建成功，但主 JavaScript 产物约 980.45 kB（gzip 310.24 kB），超过默认 500 kB 警告阈值。
- 风险：首次加载速度可能受影响；当前不影响功能联调。
- 建议：后续按页面使用动态导入，或拆分 Ant Design 等依赖 chunk；不要仅提高警告阈值掩盖问题。
- 回归证据：vite.config.ts 已按 manualChunks 拆分 react/antd，主 chunk 由约 980 KB 降至约 9 KB；antd 拆为独立缓存 chunk，首屏主包体积告警缓解。

### BLOCK-001 微信开发者工具模拟器 UI 自动化受阻

- 类型：测试工具阻断，不计为产品 Bug
- 当前状态：2026-11-13 复测仍受阻，联调计划暂停
- 已确认事实：自动化工具识别到唯一目标窗口 `lost-found-miniapp - 微信开发者工具 Stable v1.06.2504060`。
- 失败现象：第一次状态采集报 `FrameArrived timed out`；重新枚举并绑定窗口后，第二次报 `window capture timed out`。
- 2026-11-13 回归证据：再次识别到标题窗口（窗口 ID `132076`）；首次捕获报 `FrameArrived timed out`。随后重新枚举窗口并按标题精确绑定，重试仍报 `window capture timed out`。
- 2026-11-13 后续尝试：已在 VS Code 中通过 `Python: FastAPI (8000)` 调试配置启动后端，`POST /api/v1/session/wechat` 实测返回 HTTP 200。切换微信开发者工具时，桌面接口明确返回 `window is minimized`；执行一次激活、刷新窗口句柄并重读后仍为最小化状态，期间还检测到用户正在该窗口输入，因此未继续争抢控制权。
- 2026-11-13 首页视觉还原验证：微信开发者工具已恢复，模拟器显示为 `iPhone 12/13 (Pro) 63%`。首页成功编译并加载，无障碍树确认“拾光”、搜索框、轨迹卡、信息广场、招领/寻物、分类、自定义底栏等结构存在；调试器显示 `Errors: 0`。TypeScript 类型检查也已通过。首次画面捕获超时后重新枚举并精确绑定窗口，页面状态可读取，但第二次截图仍报 `window capture timed out`。
- 未验证范围：欢迎页实际显示、按钮点击、首页列表渲染、发布表单、详情页及控制台错误。
- 本轮新增未验证范围：首页与 1080×2400 基准图的像素差异、字体基线、阴影与圆角误差；当前后端列表总数为 0，信息卡片实数据状态也未完成模拟器截图验证。
- 说明：因为没有取得最新画面，未进行盲目坐标点击；这些页面不能标记为“已通过”。
- 后续条件：由用户将微信开发者工具窗口恢复为非最小化状态，并暂时停止操作该窗口后，再执行模拟器逐页测试。
- 暂停位置：首页静态编译和结构加载通过后、首次像素差异截图之前。
- 恢复后的下一项：先取得 iPhone 13 模拟器首页截图，与 `design/miniapp-home-concept.png` 逐区比较；通过后再准备固定测试数据验证信息卡片与交互。

### BLOCK-002 “我的”页自动热重载失败

- 类型：测试工具/构建链阻断，尚不能认定为产品 Bug
- 唯一编号和标题：BLOCK-002 微信开发者工具自动热重载读取 `functionalPages` 失败
- 发现时间及环境：2026-11-13 10:08（Asia/Shanghai）；微信开发者工具 Stable v1.06.2504060；基础库 3.18.0；模拟器 `iPhone 12/13 (Pro) 63%`；页面配置 `navigationStyle: custom`
- 严重度：高（阻断新设计的模拟器加载与视觉验收）
- 当前状态：待修复，联调计划暂停
- 测试前提：`miniapp/pages/my` 的 WXML、WXSS、JSON 与必要展示字段已更新；独立 PNG 图标已生成；`node node_modules/typescript/bin/tsc --noEmit` 已通过。
- 可重复操作步骤：
  1. 保持微信开发者工具开启自动热重载。
  2. 保存“我的”页 WXML、WXSS、TS、JSON 修改。
  3. 激活微信开发者工具并读取模拟器与调试器状态。
- 实际结果：调试器出现 `[自动热重载错误] Cannot read properties of undefined (reading 'functionalPages')`；调用栈位于开发者工具 `miniprogram-builder` 的 `AppGraph.getIndependentRoot`。模拟器中的 `pages/my/my` 无障碍树仍为旧版结构（旧菜单和 `›` 文本），说明本次新页面没有成功加载。
- 预期结果：自动热重载或普通编译成功，模拟器加载新版“我的档案”结构，随后可按区域生成截图并与 `design/miniapp-my-concept.png` 比较。
- 证据：开发者工具调试器显示 `Errors: 3, Warnings: 22`；其中包含上述热重载异常；当前运行时页面文本仍为“学校为用户自报，未经校方认证”“我的发布 ›”等旧版内容。
- 已确认事实：小程序 TypeScript 静态检查通过；开发者工具版本和目标窗口已确认；热重载异常真实出现；模拟器仍展示旧版页面。
- 合理推测：异常更可能来自开发者工具自动热重载/增量构建状态，因为调用栈位于工具内部；在未执行干净的普通编译并取得结果前，不能排除项目配置与该工具版本组合触发问题。
- 暂时无法验证：新版 WXSS 是否被微信编译器完整支持；新版页面实际像素、滚动、安全区、点击导航及接口计数；异常是否可通过普通编译、清缓存或重启工具消除。
- 初步定位：微信开发者工具 `miniprogram-builder` 自动热重载阶段，不是 TypeScript 类型检查阶段。
- 暂停位置：新版“我的”页静态编译通过后、首次成功加载模拟器之前。
- 修复后恢复的下一项：由修复负责人说明已处理的开发者工具实例/版本或配置后，先执行普通编译并确认新版结构加载；通过后从顶部标题与身份卡区域开始截图差异比较。

## 已通过项目

1. Python 后端隔离测试：112 passed（含新增 CORS/会话回归 3 项）。
2. 微信小程序类型检查：`node node_modules/typescript/bin/tsc --noEmit`，通过。
3. 运营后台类型检查与构建：通过。
4. Python 真实 HTTP 主链路：
   - mock 微信登录成功；
   - 用户协议确认成功；
   - 创建招领草稿成功；
   - 发布成功，状态为 `PUBLISHED`；
   - 使用类型与关键词检索成功，列表返回刚发布的同一 ID。
5. Java 后端全量测试：115 passed（需先设置 `JAVA_HOME` 指向 JDK 21）。
6. 契约测试：`contracts/run_contract_tests.py` 通过（java == python == expected）。

## 后续回归清单

1. ~~配置 JDK 21 / `JAVA_HOME`，执行 Java 全量测试。~~（已完成：115 passed）
2. ~~执行 `contracts/run_contract_tests.py`，确认 Java/Python 契约一致。~~（已完成：通过）
3. 修复 CORS 后，使用浏览器实际登录运营后台并验证举报、下架、限制账户、审计日志。
4. 修复失效会话处理后，分别在小程序和运营后台验证自动清 token、跳转登录和重新登录。
5. 恢复微信开发者工具画面采集后，完成欢迎页、首页、发布、详情、认领、消息、交接、我的页面的逐页测试。
