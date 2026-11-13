# Spec Delta

## MODIFIED Requirements

### Requirement: 公开端点不泄露私密数据

公开只读端点 SHALL NOT 返回私密核验特征、认领证据或自由消息；响应 SHALL 仅包含公开字段（`id`、`type`、`category`、`title`、`publicDescription`、`schoolId`、`schoolName`、`campusId`、`campusName`、`area`、`status`、`authorId`、`createdAt`、`updatedAt`、`images`）。

#### Scenario: 公开详情仅含公开字段

- **WHEN** 用户（含匿名）请求公开详情
- **THEN** 响应 SHALL NOT 包含私密核验特征、认领证据或自由消息
