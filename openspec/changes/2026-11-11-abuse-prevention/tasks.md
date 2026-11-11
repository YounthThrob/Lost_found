# Tasks

## 1. 契约落地

- [x] 1.1 `contracts/error-codes.yaml` 新增 RATE_LIMITED（429）、CLAIM_RATE_LIMITED（429）、ACCOUNT_RESTRICTED（403）
- [x] 1.2 新增 `contracts/rate-limits.yaml`（窗口与各操作阈值）
- [x] 1.3 mock 微信新增 `valid-code-4`（两后端），供限流契约测试

## 2. Java 实现

- [x] 2.1 新增 `abuse` 包：`RateLimiter`（固定窗口、按用户+操作计数、clear）
- [x] 2.2 `ErrorCode` 新增 RATE_LIMITED / CLAIM_RATE_LIMITED / ACCOUNT_RESTRICTED
- [x] 2.3 `UserAccount` 支持 RESTRICTED；`AgreementGuard.requireAgreed` 增加限制校验；`InMemoryUserStore` 增加 restrict/unrestrict
- [x] 2.4 `ClaimService`/`ListingService`/`CommunicationService`/`ReportService` 注入限流器并前置检查
- [x] 2.5 集成测试：认领限流、发布限流、消息限流、举报限流、独立计数、账户限制、风控细节不泄露

## 3. Python 实现

- [x] 3.1 新增 `app/abuse`（rate_limiter）；errors 新增 3 个错误码
- [x] 3.2 `UserAccount` 支持 RESTRICTED；`require_agreement` 增加限制校验；store 增加 restrict/unrestrict
- [x] 3.3 四个 service 注入限流检查
- [x] 3.4 pytest：认领限流、发布限流、消息限流、举报限流、独立计数、账户限制、风控细节不泄露

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：认领限流（CLAIM_RATE_LIMITED）、发布限流（RATE_LIMITED）探针
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/abuse-prevention/`。