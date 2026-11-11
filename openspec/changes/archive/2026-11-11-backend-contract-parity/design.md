# Design

## Context

- 八个业务 capability 已完成，每个 change 均通过 `contracts/run_contract_tests.py` 验证 `java == python == expected`。
- PRODUCT.md §12.4 定义了 7 项等价性验收：非法状态迁移、频率限制、拉黑、权限规则已被既有探针覆盖；数据库迁移等价（第 6 项）与 OpenAPI breaking-change 工具（第 7 项）在 walking skeleton 阶段无对应对象，属后续 change。
- 现存契约漂移：
  - Java 对非整数查询参数（`page=abc`）经 catch-all `Exception` 处理器返回 `INTERNAL_ERROR`(500)，Python 由 FastAPI 参数校验返回 `VALIDATION_FAILED`(422)。
  - 列表排序：Java `listVisible()` 按 `createdAt DESC, id ASC`；Python `list_visible()` 仅按 `created_at DESC`，同时间戳时顺序不一致。

## Goals / Non-Goals

**Goals:**

- 将 §12.4 中可执行的部分形式化为 `backend-contract-parity` 行为规格。
- 修复 Java 非法参数 500 的契约漂移，统一为 `VALIDATION_FAILED`(422)。
- 统一列表排序次级比较键，保证同时间戳结果顺序一致。
- 扩展契约测试覆盖分页结构/边界、非整数参数、时间戳 RFC3339 格式、错误包裹结构。

**Non-Goals:**

- 不实现数据库迁移等价（§12.4 第 6 项，需真实 DB，属持久化 change）。
- 不引入 OpenAPI breaking-change 自动检查工具（§12.4 第 7 项，属 CI/工具链 change）。
- 不新建 `contracts/test-vectors/` 目录（隐私字段不泄露已由既有探针与单元测试覆盖）。
- 不改变既有业务行为、状态机或字段名。

## Decisions

1. **等价性验收落地**：新增 `backend-contract-parity` 主 spec，将 §12.4 可执行部分转化为 SHALL 要求；`contracts/run_contract_tests.py` 是唯一验收门槛。

2. **Java 非法参数修复**：在 `GlobalExceptionHandler` 增加 `MethodArgumentTypeMismatchException`（`TypeMismatchException`）处理器，映射到 `VALIDATION_FAILED`(422)。该异常由 Spring 在参数绑定阶段抛出，此前落入 catch-all `Exception` 处理器返回 500。此修复仅影响分页整型参数的非法输入，不影响正常客户端。

3. **排序次级比较键**：Python `list_visible()` 补 `id` 次级键（`key=lambda p: (p.created_at, p.id)` 倒序），与 Java `Comparator.comparing(createdAt, reverseOrder()).thenComparing(id)` 对齐。主键仍为 `created_at` 倒序，行为不变，仅消除同时间戳的顺序歧义。

4. **契约测试探针**：
   - 分页结构：在 `listing_search_published` 探针补 `pagination` 整对象校验（`page/pageSize/total/hasMore`）。
   - 分页边界：新增 `pagination_page_zero`、`pagination_page_size_overflow`、`pagination_non_integer_page`（均 422）与 `pagination_beyond_last_page`（200 空 items）。
   - 时间戳格式：在 `login_success` 探针补 `timestamp_is_rfc3339`（正则 `\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z`）。
   - 错误包裹结构：在 `login_invalid` 探针补 `error_keys`（code/message/details）与 `details_empty`、`has_request_id` 校验。

5. **契约零增量**：本 change 不新增/修改接口字段或错误码，故 `contracts/error-codes.yaml`、`openapi/product-api.yaml` 无需变更。

## Risks / Trade-offs

- [非整数参数] 该行为此前 Java 为 500、Python 为 422，修复属契约收敛；正常客户端不会发送非整数分页，无兼容性风险。
- [排序 tiebreaker] Python 补 id 次级键使顺序更确定，无同时间戳场景下无感。
- [DB/工具链等价延后] §12.4 第 6、7 项依赖真实数据库与 CI 工具链，walking skeleton 阶段暂无对象，延后至持久化与部署 change。

## Migration Plan

- 无既有数据迁移。仅改 Java 异常处理器与 Python 排序逻辑，并扩展契约测试探针。
