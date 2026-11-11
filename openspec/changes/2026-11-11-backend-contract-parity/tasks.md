# Tasks

## 1. 契约落地

- [ ] 1.1 新增 `openspec/changes/2026-11-11-backend-contract-parity/` 的 proposal/design/tasks 与 spec delta

## 2. Java 修复

- [ ] 2.1 `GlobalExceptionHandler` 增加 `MethodArgumentTypeMismatchException` 处理器，映射到 `VALIDATION_FAILED`(422)
- [ ] 2.2 单元测试：非整数分页参数返回 422

## 3. Python 修复

- [ ] 3.1 `listing/store.py` 的 `list_visible()` 补 `id` 次级排序键

## 4. 契约测试扩展

- [ ] 4.1 补充分页结构探针（`page/pageSize/total/hasMore`）
- [ ] 4.2 补充分页边界探针（`page=0`、`pageSize=101`、非整数 `page`、越界 `page`）
- [ ] 4.3 补充时间戳 RFC3339 格式探针与错误包裹结构探针
- [ ] 4.4 对两后端运行契约测试，验证 `java == python == expected`

## Workflow follow-up

- 全部通过后归档本 change，将 delta 合并进 `openspec/specs/backend-contract-parity/`。
