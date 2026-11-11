# Tasks

## 1. 契约落地

- [x] 1.1 `contracts/error-codes.yaml` 新增 CLUE_NOT_ALLOWED（409）
- [x] 1.2 `contracts/rate-limits.yaml` limits 增加 clue（20/60s）
- [x] 1.3 `openapi/product-api.yaml` 新增 3 个端点与 ClueCreate/ClueDecide/ClueView/ClueListData/ClueResponse/ClueListResponse

## 2. Java 实现

- [x] 2.1 新增 `clue` 包：Clue 记录、InMemoryClueStore、ClueService、ClueController
- [x] 2.2 `ErrorCode` 新增 CLUE_NOT_ALLOWED
- [x] 2.3 ClueService 接入协议守卫、限流（clue）、类型/可见性校验、违禁词检测、状态机
- [x] 2.4 集成测试：LOST 提交、FOUND 拒绝、非展示中拒绝、违禁词 REJECTED、失主查看、非失主 404、判定状态机、重复判定 409、限流

## 3. Python 实现

- [x] 3.1 新增 `app/clue`（store、service、router）；errors 新增 CLUE_NOT_ALLOWED
- [x] 3.2 接入 require_agreement、限流、类型/可见性校验、违禁词检测、状态机
- [x] 3.3 pytest：同 Java 覆盖项

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：线索提交/限制/查看/判定/审核/限流探针
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/clue-tips/`。
