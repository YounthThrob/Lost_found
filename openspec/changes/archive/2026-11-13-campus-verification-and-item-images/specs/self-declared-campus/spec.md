# Spec Delta

## MODIFIED Requirements

### Requirement: 自报学校标记为非认证

系统 SHALL 将自报学校的核验状态标记为 `SELF_DECLARED`（未核验）或 `EMAIL_VERIFIED`（已核验校园邮箱），且 SHALL NOT 返回任何表示「官方认证」「校方认证」或「身份可信」的字段。

#### Scenario: 响应携带非认证标记

- **WHEN** 用户查询自报学校
- **THEN** 响应中 `verificationStatus` 为 `SELF_DECLARED` 或 `EMAIL_VERIFIED`

#### Scenario: 响应不暗示认证

- **WHEN** 用户查询自报学校
- **THEN** 响应 SHALL NOT 包含表示官方/校方认证的结论字段（如 `certified`、`officiallyVerified`）
