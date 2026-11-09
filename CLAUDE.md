# CLAUDE.md

> 本文件是项目的工程约束，供 Claude Code 每次会话自动加载。产品与技术细节以 `PRODUCT.md` 为准。

## 项目概述

- 校园失物招领互助小程序（**非官方**），目标终端：微信小程序 + 运营管理 Web 端。
- 后端提供 **Java（Spring Boot 3）与 Python（FastAPI）两个功能等价**的独立实现，生产按部署二选一。
- 产品与技术方案唯一事实源：[PRODUCT.md](PRODUCT.md)。

## 不可违反的约束

1. **契约优先**：OpenAPI（`openapi/product-api.yaml`）、错误码、状态机、测试向量是共同事实源。新增接口、改状态迁移或改字段，必须先改契约，再改实现。
2. **双后端等价**：Java 与 Python 对同一请求必须返回一致的 HTTP 状态码、业务错误码、字段名、分页格式与时间格式。任一实现新增能力须先过兼容性评审（见 PRODUCT.md §12.4）。
3. **隐私红线**：私密核验特征（PrivateClue）、认领证据（ClaimEvidence）、自由消息**不得**进入公开接口、普通日志、埋点、搜索索引、缓存或分析系统。
4. **非官方边界**：文案、命名、视觉不得暗示“校方认证 / 身份可信 / 官方认证”；自报学校必须标注“用户自报，未经校方认证”。
5. **状态机由服务端校验**：客户端不得任意指定目标状态（见 PRODUCT.md §9）。
6. **私密数据应用层加密**：AES-256-GCM + KMS 托管主密钥，密钥不落库、不硬编码、不进配置文件。

## 统一规范

- 时间：UTC，RFC3339（如 `2026-11-09T08:00:00Z`）。
- 分页：`page`（从 1 起）/ `pageSize`（默认 20，上限 100）。
- 错误码：见 `contracts/error-codes.yaml`，核心码见 PRODUCT.md §11.2。
- 主键：UUIDv7 或等价不可预测标识。
- 语言：文档、commit 与 OpenSpec artifacts 用中文；OpenSpec 结构标题与 SHALL/MUST 等关键词保留英文。

## 仓库结构（规划，见 PRODUCT.md §12.1）

```text
openapi/  contracts/  backend-java/  backend-python/  miniapp/  admin-web/  deploy/
```

## 工作流

- 已初始化 OpenSpec（`spec-driven` schema）。新功能先 `/opsx:propose` 生成 change，再 `/opsx:apply` 实现，最后 `/opsx:archive` 归档。
- capability 拆分与实施顺序见 PRODUCT.md §17；MVP 需求见 §8（REQ-001~010）。

## 技术栈

- Java 21 LTS · Spring Boot 3 · Spring Data JPA · Flyway · springdoc-openapi。
- Python 3.12+ · FastAPI · SQLAlchemy 2 · Pydantic 2 · Alembic。
- 前端：微信小程序 `miniapp/`、运营后台 `admin-web/`。
