# 校园失物招领（非官方）· Python 后端

FastAPI + 内存存储的 walking skeleton 实现，与 `backend-java` 功能等价。

## 运行测试

```bash
# 必须在 backend-python 目录下执行
uv run --extra test python -m pytest -q
```

> 注意：不要直接 `python -m pytest`，那会用到 Anaconda 全局依赖（旧版 Starlette 与新版 httpx 不兼容，会在收集阶段报 `Client.__init__() got an unexpected keyword argument 'app'`）。也不要 `uv run --extra test pytest -q`（在 Windows 上会调用到错误的 pytest 入口，报 `No module named 'app'`）。

## 启动服务

```bash
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## 跨域来源

通过环境变量 `CORS_ORIGINS` 配置运营后台来源白名单（逗号分隔），默认允许 `http://localhost:5173,http://127.0.0.1:5173`。生产环境请指向真实后台域名，禁用通配来源。
