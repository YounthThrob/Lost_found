# Design

## Context

- 既有 `user-account` 已实现微信登录与会话，登录返回 `{data:{token}}`，`GET /me` 返回 `{data:{id,status}}`。
- REQ-001：首次使用必须确认用户协议、隐私说明及非官方平台提示；未确认协议则禁止发布/留言/认领（错误码 `AGREEMENT_NOT_ACCEPTED`，403）。
- 受限操作端点（发布/认领/留言）在后续 change（C06/C14/C16）才实现。

## Goals / Non-Goals

**Goals:**

- 账户可表达「已确认协议」状态，并在登录与 `/me` 中暴露，供客户端引导首次确认。
- 提供幂等的协议确认接口，记录确认状态与时间。
- 落地可复用的受限操作门控机制，并定义 `AGREEMENT_NOT_ACCEPTED` 语义。

**Non-Goals:**

- 不实现资料编辑（C03）、注销（C04）、发布（C06）等。
- 不在本 change 把门控接到尚不存在的发布/认领/留言端点。
- 不校验用户是否「阅读」了协议正文（仅记录确认动作，协议文本与前端交互归小程序端）。

## Decisions

1. **协议状态对外暴露布尔 `agreementAccepted`，服务端以 `agreement_accepted_at`（RFC3339 时间戳，可空）记录**。理由：对外布尔足以让客户端判断是否需展示协议页；记录时间满足「记录确认状态与时间」，且不在响应中返回时间，避免双后端时间戳不一致破坏黑盒契约测试的等价性断言。

2. **确认接口 `POST /api/v1/me/agreement` 幂等**：已确认用户重复调用仍返回 200 与 `agreementAccepted: true`，不覆盖首次确认时间。

3. **门控机制**：
   - Java：`@RequiresAgreement` 注解 + `HandlerInterceptor` 拦截标注方法，未确认则抛 `AGREEMENT_NOT_ACCEPTED`。
   - Python：`require_agreement` FastAPI 依赖，链式调用 `get_current_user` 后校验协议状态。
   - 两端门控逻辑等价，作为受限端点的可复用守卫。

4. **门控规则的验证落点**：受限操作端点尚未存在，故门控「拒绝未确认用户」的行为在本 change 通过单元/集成测试验证守卫组件（给定未确认用户 → 抛 `AGREEMENT_NOT_ACCEPTED`），HTTP 层面的端到端拒绝随 C06 等端点接入时由契约测试覆盖。

5. **错误码**：`contracts/error-codes.yaml` 新增 `AGREEMENT_NOT_ACCEPTED`（HTTP 403，message「未确认用户协议，禁止发布/留言/认领」），与 PRODUCT.md §11.2 一致。

## Risks / Trade-offs

- [门控在本 change 无可端到端验证的受限端点] → 以守卫组件单元测试证明拒绝行为，端点接入（C06 等）时补契约测试；在 tasks 中明确标注。
- [协议文本与前端交互不在后端范围] → 后端仅记录确认动作，协议内容与展示由小程序端维护，后端不承载协议文本（避免双后端重复维护）。
- [双后端行为漂移] → 契约测试断言两后端对同一请求返回一致的 `agreementAccepted` 与错误码。

## Migration Plan

- 无既有数据迁移（in-memory 存储）。新账户默认 `agreementAccepted=false`；已有账户视为未确认，前端会引导其补确认。
