# Tasks

## 1. 契约落地

- [ ] 1.1 `contracts/error-codes.yaml` 新增 BLOCKED（403）
- [ ] 1.2 `openapi/product-api.yaml`：新增 blocks、me/blocks、conversations/{id}/messages 端点与 Block/Message schema

## 2. Java 实现

- [ ] 2.1 新增 `communication` 包：Block/Message record、BlockStore、MessageStore、CommunicationService、CommunicationController
- [ ] 2.2 `ErrorCode` 新增 BLOCKED
- [ ] 2.3 集成测试：拉黑/重复/自己/不存在/黑名单/解除/发送/查看/被拉黑/越权/未接受/非法类型/空内容

## 3. Python 实现

- [ ] 3.1 新增 `app/communication`（store/service/router）；errors 新增 BLOCKED；main 注册路由
- [ ] 3.2 pytest：拉黑/重复/自己/不存在/黑名单/解除/发送/查看/被拉黑/越权/未接受/非法类型/空内容

## 4. 契约测试与等价性

- [ ] 4.1 扩展 `contracts/run_contract_tests.py`：发送/查看/拉黑/重复/自己/不存在/黑名单/被拉黑/解除/越权/未接受/非法类型/空内容
- [ ] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/safe-communication/`。
