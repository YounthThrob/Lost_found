# Tasks

## 1. 契约落地

- [ ] 1.1 `contracts/error-codes.yaml` 新增 HANDOVER_ALREADY_EXISTS（409）
- [ ] 1.2 `openapi/product-api.yaml`：ItemPostView.status 加入 COMPLETED；ClaimView.status 加入 COMPLETED/CANCELLED；新增 3 个 handover 端点与 Handover schema

## 2. Java 实现

- [ ] 2.1 新增 `handover` 包：Handover record、HandoverStore、HandoverService、HandoverController
- [ ] 2.2 `ErrorCode` 新增 HANDOVER_ALREADY_EXISTS
- [ ] 2.3 集成测试：创建/重复/非参与者/未接受/单方确认/双方确认/终止/非法状态/状态联动/字段校验

## 3. Python 实现

- [ ] 3.1 新增 `app/handover`（store/service/router）；errors 新增 HANDOVER_ALREADY_EXISTS；main 注册路由
- [ ] 3.2 pytest：创建/重复/非参与者/未接受/单方确认/双方确认/终止/非法状态/状态联动/字段校验

## 4. 契约测试与等价性

- [ ] 4.1 扩展 `contracts/run_contract_tests.py`：创建/重复/非参与者/未接受/单方确认/双方确认/终止/非法状态/状态联动/字段校验
- [ ] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/item-handover/`。
