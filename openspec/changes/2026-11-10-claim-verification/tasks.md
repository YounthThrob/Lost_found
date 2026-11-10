# Tasks

## 1. 契约落地

- [ ] 1.1 `contracts/error-codes.yaml` 新增 CLAIM_DUPLICATE、POST_NOT_CLAIMABLE、HANDOVER_CONFLICT
- [ ] 1.2 `openapi/product-api.yaml`：ItemPostView.status 加入 VERIFYING/PENDING_HANDOVER；新增 5 个 claim 端点与 Claim schema

## 2. Java 实现

- [ ] 2.1 新增 `claim` 包：Claim record、ClaimStore、ClaimService、ClaimController
- [ ] 2.2 `ErrorCode` 新增三个认领错误码；ItemPostStore.listPublished 改为 listVisible（含 VERIFYING）；getDetail 可见性扩展
- [ ] 2.3 集成测试：提交/重复/不可认领/发布者查看/越权/请求补充/接受/拒绝/信息联动/待交接冲突

## 3. Python 实现

- [ ] 3.1 新增 `app/claim`（store/service/router）；errors 新增三个错误码
- [ ] 3.2 `listing.store.list_published` 改为 list_visible；`listing.service.get_detail` 可见性扩展
- [ ] 3.3 pytest：提交/重复/不可认领/发布者查看/越权/请求补充/接受/拒绝/信息联动/待交接冲突

## 4. 契约测试与等价性

- [ ] 4.1 扩展 `contracts/run_contract_tests.py`：认领提交/重复/不可认领/查看/越权/补充/接受/拒绝/信息联动/待交接冲突
- [ ] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/claim-verification/` 与 `openspec/specs/lost-found-listing/`。
