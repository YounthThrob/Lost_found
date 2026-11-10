# Spec Delta

## Purpose

定义用户拉黑/解除拉黑/查看黑名单，以及已接受认领申请的双方交换有限消息（结构化消息与文本留言）的规则，确保消息仅对会话双方可见、被拉黑后无法继续发送消息、状态与身份均由服务端校验。

## ADDED Requirements

### Requirement: 拉黑用户

用户 SHALL 能够拉黑另一用户，拉黑为幂等操作。系统 SHALL 拒绝拉黑自己或不存在用户。`blockedUserId` SHALL 为内部用户标识（UUID，非 openid）。

#### Scenario: 拉黑成功

- **WHEN** 用户拉黑另一存在用户
- **THEN** 系统保存拉黑关系并返回 `blockedUserId` 与 `createdAt`

#### Scenario: 重复拉黑幂等

- **WHEN** 用户重复拉黑已拉黑的用户
- **THEN** 系统返回已有拉黑关系，不产生重复记录

#### Scenario: 拉黑自己被拒绝

- **WHEN** 用户尝试拉黑自己
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 拉黑不存在用户被拒绝

- **WHEN** 用户尝试拉黑不存在的用户标识
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）且不保存

### Requirement: 解除拉黑

用户 SHALL 能够解除对另一用户的拉黑，解除为幂等操作。

#### Scenario: 解除拉黑成功

- **WHEN** 用户解除对已拉黑用户的拉黑
- **THEN** 系统移除拉黑关系

#### Scenario: 解除未拉黑用户幂等

- **WHEN** 用户解除对未拉黑用户的拉黑
- **THEN** 系统不报错，保持无拉黑关系

### Requirement: 查看黑名单

用户 SHALL 能够查看自己拉黑的用户列表，结果分页返回；其他用户 SHALL NOT 查看。

#### Scenario: 查看成功

- **WHEN** 用户查询自己的黑名单
- **THEN** 系统返回被拉黑用户列表与分页信息

#### Scenario: 仅本人可见

- **WHEN** 任意用户查询
- **THEN** 系统仅返回当前用户自己的黑名单

### Requirement: 发送有限消息

已接受认领申请的信息发布者与申请人 SHALL 能够通过会话（`conversationId` 即 `claimId`）互发有限消息。系统 SHALL 校验发送者参与身份、申请已接受、消息类型合法、内容非空且被对方拉黑时拒绝发送。

#### Scenario: 发送成功

- **WHEN** 会话参与者在申请已接受后发送合法消息
- **THEN** 系统保存消息并返回 `senderId`、`type`、`content` 与 `createdAt`

#### Scenario: 未确认协议被拒绝

- **WHEN** 未确认协议的用户尝试发送消息
- **THEN** 系统返回 `AGREEMENT_NOT_ACCEPTED`（HTTP 403）且不保存

#### Scenario: 非参与者被拒绝

- **WHEN** 非会话参与者（既非发布者也非申请人）尝试发送或查看消息
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

#### Scenario: 申请未接受被拒绝

- **WHEN** 会话对应申请尚未进入 `ACCEPTED` 状态
- **THEN** 系统返回 `INVALID_STATE_TRANSITION`（HTTP 409）且不保存

#### Scenario: 被拉黑后发送被拒绝

- **WHEN** 会话中对方已拉黑发送者，发送者继续发送消息
- **THEN** 系统返回 `BLOCKED`（HTTP 403）且不保存

#### Scenario: 消息类型非法被拒绝

- **WHEN** 发送者提交非法的消息 `type`
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 消息内容为空被拒绝

- **WHEN** 发送者提交空或超长的 `content`
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 查看会话消息

会话参与者 SHALL 能够查看会话内全部消息，结果分页返回并按时间倒序；非参与者 SHALL NOT 查看。

#### Scenario: 参与者查看成功

- **WHEN** 会话参与者查询该会话消息
- **THEN** 系统返回消息列表与分页信息

#### Scenario: 非参与者不可查看

- **WHEN** 非会话参与者尝试查看会话消息
- **THEN** 系统返回 `NOT_FOUND`（HTTP 404）

### Requirement: 消息与黑名单隐私保护

消息内容与发送者标识、黑名单内容 SHALL 仅对相关参与者或拉黑者本人可见，SHALL NOT 出现在公开信息、普通日志或任何公开接口中。

#### Scenario: 公开接口不泄露消息与黑名单

- **WHEN** 任意用户查询公开信息列表或详情
- **THEN** 响应 SHALL NOT 包含任何消息内容、`senderId` 或黑名单记录
