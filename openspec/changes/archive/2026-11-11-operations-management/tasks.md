# Tasks

## 1. 契约落地

- [x] 1.1 `contracts/error-codes.yaml` 新增 OPERATOR_REQUIRED（403）
- [x] 1.2 `openapi/product-api.yaml`：ItemPostView.status 枚举新增 REMOVED
- [x] 1.3 `openapi/product-api.yaml` 新增 5 个 admin 端点与 AdminActionRequest/UserStatusView/UserStatusResponse/RemovePostResponse/AuditEventView/AuditEventListData/AuditEventListResponse

## 2. Java 实现

- [x] 2.1 `UserAccount` 增 `role` 字段；`InMemoryUserStore` 相关构造点补充 role
- [x] 2.2 `WechatClient` mock 新增 operator-code；`AccountService.login` 按 openid 判定角色
- [x] 2.3 `ErrorCode` 新增 OPERATOR_REQUIRED
- [x] 2.4 `ListingService` 增 `remove`（PUBLISHED/VERIFYING/PENDING_HANDOVER → REMOVED）
- [x] 2.5 `ReportStore` 增 `listAll`
- [x] 2.6 新增 `operations` 包：AuditEvent、AuditStore、OperationsService、OperationsController、OperatorGuard
- [x] 2.7 集成测试：运营鉴权、下架、非法状态、限制/解除、举报列表、审计日志、空 reason

## 3. Python 实现

- [x] 3.1 `UserAccount` 增 role；`store.py` 相关构造点补充 role
- [x] 3.2 `wechat_client.py` 新增 operator-code；`service.login` 按 openid 判定角色
- [x] 3.3 `errors.py` 新增 OPERATOR_REQUIRED
- [x] 3.4 `listing/service.py` 增 `remove`
- [x] 3.5 `moderation/store.py` 增 `list_all`
- [x] 3.6 新增 `app/operations`（store、service、router、deps）；`main.py` 挂载路由
- [x] 3.7 pytest：同 Java 覆盖项

## 4. 契约测试与等价性

- [x] 4.1 扩展 `contracts/run_contract_tests.py`：运营鉴权/下架/限制/解除/举报列表/审计日志探针
- [x] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/operations-management/`。
