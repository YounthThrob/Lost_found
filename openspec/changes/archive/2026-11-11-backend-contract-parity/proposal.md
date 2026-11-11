# Proposal

## Why

八个业务 capability 已全部落地，且 Java 与 Python 两后端在每个 change 中均通过同一套黑盒契约测试（`contracts/run_contract_tests.py`）。但「契约一致性」本身尚未作为独立能力被形式化，且存在契约测试未覆盖的等价性盲区：分页结构/边界、时间戳格式、以及非法请求参数在两后端是否返回一致错误码。经核查，当前存在一处契约漂移：Java 对非整数查询参数（如 `page=abc`）经 catch-all 返回 `INTERNAL_ERROR`(500)，而 Python 由 FastAPI 校验返回 `VALIDATION_FAILED`(422)。本 change 落地 `backend-contract-parity` capability（PRODUCT.md §17，横跨全部能力），把 PRODUCT.md §12.4 的等价性验收落到可执行的契约测试，并收敛发现的契约漂移。

## What Changes

- 新增 `backend-contract-parity` 行为规格：统一响应包裹与 RFC3339 时间、分页契约、请求参数绑定错误一致性、黑盒契约测试作为等价性验收门槛。
- 修复 Java 非法请求参数（非整数 `page`/`pageSize`）返回 500 的契约漂移，统一为 `VALIDATION_FAILED`(422)，与 Python 一致。
- 统一列表排序的次级比较键：Python `list_visible()` 补 `id` 次级键，与 Java `createdAt DESC + id ASC` 对齐，保证同时间戳结果顺序一致。
- 扩展 `contracts/run_contract_tests.py`：分页结构（`page/pageSize/total/hasMore`）与边界（`page=0`、`pageSize` 超限、非整数 `page`、越界页码）、时间戳 RFC3339 格式、错误包裹结构探针。

## Capabilities

### New Capabilities

- `backend-contract-parity`: 两后端对同一请求返回一致的状态码、业务错误码、字段名、分页格式与时间格式，并以共同黑盒契约测试作为验收门槛。

### Modified Capabilities

- 无（本 change 不改变既有业务行为或状态机，仅收敛两实现间的契约漂移）。

## Impact

- 修改 `backend-java/.../shared/GlobalExceptionHandler.java`：新增参数类型不匹配处理器，映射到 `VALIDATION_FAILED`(422)。
- 修改 `backend-python/app/listing/store.py`：列表排序补 `id` 次级比较键。
- 扩展 `contracts/run_contract_tests.py`：新增分页、时间戳、错误包裹探针。
- `contracts/error-codes.yaml` 与 `openapi/product-api.yaml` 无需变更（本 change 不新增字段或错误码，仅收敛行为一致）。
