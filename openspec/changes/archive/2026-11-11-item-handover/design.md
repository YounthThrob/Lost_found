# Design

## Context

- 已有 `claim-verification`（接受后申请 ACCEPTED、信息 PENDING_HANDOVER）、`safe-communication`（会话=claimId 的有限消息 + 拉黑）。
- PRODUCT.md §9.1：`PENDING_HANDOVER → COMPLETED`（双方确认）、`PENDING_HANDOVER → PUBLISHED 或 CLOSED`（交接终止）。
- PRODUCT.md §9.2：申请 `ACCEPTED → PENDING_HANDOVER → COMPLETED`，终态 `CANCELLED`。
- PRODUCT.md §10：`Handover(id/claim_id/proposer_id/time_window/public_place/owner_confirmed_at/claimant_confirmed_at/status)`。
- PRODUCT.md §11：`POST /handovers`、`POST /handovers/{id}/confirm`、`POST /handovers/{id}/cancel`；REQ-007。

## Goals / Non-Goals

**Goals:**

- 发布者或申请人为已接受申请创建交接安排（时间窗口 + 公共区域）。
- 双方分别确认；双方均确认后完成交接并联动关闭信息与申请。
- 任一方终止交接；终止后信息回到可认领状态。
- 交接内容仅对交接双方可见。

**Non-Goals:**

- 不实现交接的「建议/同意」多轮协商（时间地点由创建者一次性提出，双方仅确认）。
- 不实现交接争议标记、终止原因的结构化枚举与运营介入，属后续 change。
- 不实现交接提醒订阅消息与超时自动取消（EXPIRED）。
- 终止去向固定为 `PUBLISHED`，暂不实现「终止后关闭（CLOSED）」分支。

## Decisions

1. **交接模型**：`Handover(id, claimId, proposerId, timeWindow, publicPlace, ownerConfirmedAt, claimantConfirmedAt, status, createdAt)`；`status ∈ {PENDING, COMPLETED, CANCELLED}`；`ownerConfirmedAt`/`claimantConfirmedAt` 初值为 null。MVP 将 §9.2 的申请 `PENDING_HANDOVER` 折叠进「申请 ACCEPTED + 交接 PENDING」——申请保持 `ACCEPTED`，交接实体承载「待交接」生命周期。

2. **创建**：仅信息发布者或申请人可为 `ACCEPTED` 申请创建交接安排；同一申请至多一个交接（重复创建 `HANDOVER_ALREADY_EXISTS`）；`timeWindow`/`publicPlace` 非空 ≤200；创建后交接 `PENDING`，申请与信息状态不变（信息已为 `PENDING_HANDOVER`）。

3. **确认**：`POST /handovers/{id}/confirm` 依据当前用户角色记录 `ownerConfirmedAt` 或 `claimantConfirmedAt`（重复确认幂等）；仅当两者均非空时交接迁移 `COMPLETED`，并联动申请 `ACCEPTED → COMPLETED`、信息 `PENDING_HANDOVER → COMPLETED`。单方确认不改变交接/申请/信息状态。

4. **终止**：任一方可终止 `PENDING` 交接；交接迁移 `CANCELLED`，申请 `ACCEPTED → CANCELLED`，信息 `PENDING_HANDOVER → PUBLISHED`。

5. **鉴权**：创建要求协议确认（属协调动作）；确认/终止要求有效会话 + 参与者身份（非参与者 `NOT_FOUND`）。

6. **隐私**：`timeWindow`/`publicPlace` 与各确认时间仅对发布者与申请人可见；`proposerId` 为 UUID；内存存储不落日志。

## Risks / Trade-offs

- [申请状态折叠] MVP 不引入申请 `PENDING_HANDOVER` 状态，`ACCEPTED` 直接跳 `COMPLETED`/`CANCELLED`；与 §9.2 完整状态机存在表述差异，正式版可拆分。
- [终止去向单一] 终止固定回 `PUBLISHED`，未实现 `CLOSED` 分支与原因枚举；后续 change 补充。
- [无争议/审计] 交接争议与不可删除记录属 `abuse-prevention` 与运营切片。
- [无持久化] 内存存储，重启即失；随持久化 change 落地。

## Migration Plan

- 无既有数据迁移（内存存储）。新增 `HANDOVER_ALREADY_EXISTS` 错误码与 handover schema 为契约增量。
