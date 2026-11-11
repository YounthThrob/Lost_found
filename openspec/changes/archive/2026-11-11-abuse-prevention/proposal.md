# Proposal

## Why

前序能力已覆盖账户、发布、认领、沟通、交接与内容审核，但平台仍缺少对「滥用行为」的主动防护：用户可批量提交认领申请、刷信息、刷消息或刷举报，且被运营限制的账户仍能继续操作。本 change 落地 `abuse-prevention` capability（PRODUCT.md §17 顺序 8）的 MVP 切片：对认领、发布、消息与举报实施按用户维度的频率限制，并对限制状态账户拒绝受限操作，同时确保风控细节不泄露。

## What Changes

- 新增频率限制：对认领申请、发布信息、发送消息、提交举报四类操作按用户维度做固定窗口限流，认领超限返回 `CLAIM_RATE_LIMITED`（429），其余返回 `RATE_LIMITED`（429）。
- 新增账户限制：`UserAccount.status` 扩展 `RESTRICTED` 状态；对处于限制状态的账户拒绝发布、留言、认领，返回 `ACCOUNT_RESTRICTED`（403）。
- 新增业务错误码 `RATE_LIMITED`、`CLAIM_RATE_LIMITED`、`ACCOUNT_RESTRICTED`。
- 新增共享限流配置 `contracts/rate-limits.yaml`（固定窗口与各操作阈值）。
- 风控错误响应仅含错误码与通用说明，不泄露阈值、窗口或规则细节。
- Java 与 Python 两后端等价实现。

## Capabilities

### New Capabilities

- `abuse-prevention`: 受限操作的频率限制、账户限制的强制执行，及风控细节保护。

### Modified Capabilities

- `claim-verification`：认领提交新增频率限制检查（`CLAIM_RATE_LIMITED`）。
- `lost-found-listing`：发布新增频率限制检查（`RATE_LIMITED`）。
- `safe-communication`：发送消息新增频率限制检查（`RATE_LIMITED`）。
- `content-moderation`：提交举报新增频率限制检查（`RATE_LIMITED`）。
- `user-account`：账户新增 `RESTRICTED` 状态与受限操作守卫（`ACCOUNT_RESTRICTED`）。

## Impact

- 修改 `contracts/error-codes.yaml`：新增 `RATE_LIMITED`（429）、`CLAIM_RATE_LIMITED`（429）、`ACCOUNT_RESTRICTED`（403）。
- 新增 `contracts/rate-limits.yaml`：固定窗口与各操作阈值（共同事实源）。
- 修改 `backend-java/` 与 `backend-python/`：新增限流器组件，注入认领/发布/沟通/举报服务；账户守卫扩展限制状态校验；`ErrorCode` 新增 3 个错误码；mock 微信新增 `valid-code-4` 供限流契约测试使用。
- 扩展 `contracts/run_contract_tests.py`：认领限流（CLAIM_RATE_LIMITED）、发布限流（RATE_LIMITED）等频率限制探针。
