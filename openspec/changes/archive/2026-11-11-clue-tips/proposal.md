# Proposal

## Why

后端 MVP 已覆盖账户、发布、认领、沟通、交接、举报与限流，但 §8 的 REQ-009「线索提供」尚未落地：用户无法向寻物信息提交线索，失主也无法查看与判定线索。本 change 落地 `clue-tips` capability（PRODUCT.md REQ-009）的 MVP 切片：对展示中的寻物信息提交线索、失主查看与判定线索，并落实「仅寻物可提交、仅失主可见、内容审核 + 频率限制」的隐私与反滥用边界。

## What Changes

- 新增 3 个契约端点：`POST /posts/{postId}/clues`（提交线索）、`GET /posts/{postId}/clues`（失主查看待处理线索）、`POST /clues/{clueId}/decide`（标记有效/无效）。
- 新增线索状态机（PRODUCT.md §9.3）：`SUBMITTED → ACCEPTED → CLOSED`，终态 `REJECTED`（内容审核）、`EXPIRED`（后续联动）。
- 新增业务错误码 `CLUE_NOT_ALLOWED`（409）：对招领（FOUND）信息提交线索时拒绝。
- 新增共享限流配置：`contracts/rate-limits.yaml` 增加 `clue` 操作阈值。
- 线索内容提交时通过违禁关键词检测，命中即保存为 `REJECTED` 且不出现在待处理列表；线索仅失主可见，不进入任何公开接口。
- Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `clue-tips`：向寻物信息提交线索、失主查看与判定线索、线索状态机与可见性边界。

### Modified Capabilities

- `abuse-prevention`：限流配置新增 `clue` 操作（仅共享配置增量，行为不变）。

## Impact

- 修改 `contracts/error-codes.yaml`：新增 `CLUE_NOT_ALLOWED`（409）。
- 修改 `contracts/rate-limits.yaml`：`limits` 增加 `clue`。
- 修改 `openapi/product-api.yaml`：新增 3 个端点与 `ClueCreate`、`ClueDecide`、`ClueView`、`ClueListData`、`ClueResponse`、`ClueListResponse` schema。
- 修改 `backend-java/` 与 `backend-python/`：新增 clue 存储、service、controller/router；复用限流器与违禁词检测；新增错误码。
- 扩展 `contracts/run_contract_tests.py`：线索提交、仅寻物限制、失主查看、判定状态机、内容审核、限流等探针。
