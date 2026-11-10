# Tasks

## 1. 契约落地

- [ ] 1.1 新增 `contracts/moderation-rules.yaml` 违禁词表
- [ ] 1.2 在 `openapi/product-api.yaml` 加入 REJECTED 状态、新增 `/reports`、`/reports/me` 与 Report schema

## 2. Java 实现

- [ ] 2.1 新增 `ContentModerator`（违禁词匹配）与 `Report`/`ReportStore`
- [ ] 2.2 `ListingService.publish` 接入检测器，命中则 REJECTED
- [ ] 2.3 新增 `ReportController`（POST /reports、GET /reports/me）
- [ ] 2.4 集成测试：违禁发布被拒/未命中通过/被拒不可搜索/举报提交/非法类型/我的举报

## 3. Python 实现

- [ ] 3.1 新增 `app/moderation`（检测器、Report store/service/router）
- [ ] 3.2 `listing.service.publish` 接入检测器，命中则 REJECTED
- [ ] 3.3 pytest：违禁发布被拒/未命中通过/被拒不可搜索/举报提交/非法类型/我的举报

## 4. 契约测试与等价性

- [ ] 4.1 扩展 `contracts/run_contract_tests.py`：违禁发布 REJECTED、被拒不可搜索、举报提交、非法类型 422、我的举报
- [ ] 4.2 分别对 backend-java 与 backend-python 运行契约测试，验证两后端一致

## Workflow follow-up

- 实现与契约测试全部通过后，归档本 change，将 delta 合并进 `openspec/specs/content-moderation/` 与 `openspec/specs/lost-found-listing/`。
