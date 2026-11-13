# Spec Delta

## MODIFIED Requirements

### Requirement: 用户设置自报学校与校区

已登录用户 SHALL 能够通过更新接口从目录选择学校（必填）与校区（可选）设置自报归属。系统 SHALL 校验 schoolId 指向已启用学校、campusId 指向该校已审核通过的校区，且 SHALL NOT 以自由文本接受学校或校区名称。

#### Scenario: 首次设置学校与校区

- **WHEN** 用户提交合法的 schoolId 与（可选）campusId
- **THEN** 系统保存并返回自报归属（含 schoolId/schoolName 及可选 campusId/campusName）

#### Scenario: 修改自报学校

- **WHEN** 用户提交新的 schoolId 或 campusId
- **THEN** 系统覆盖旧值并返回更新后的信息

#### Scenario: 学校为空被拒绝

- **WHEN** 用户提交缺失或为空的 schoolId
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 学校 ID 无效被拒绝

- **WHEN** 用户提交不存在或已停用的 schoolId
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

#### Scenario: 校区不属于学校被拒绝

- **WHEN** 用户提交不属于该 schoolId 的 campusId
- **THEN** 系统返回 `VALIDATION_FAILED`（HTTP 422）且不保存

### Requirement: 查询自报学校与校区

用户 SHALL 能够查询自己的自报学校与校区（以目录实体形式返回）。

#### Scenario: 未设置时返回空

- **WHEN** 用户尚未设置学校
- **THEN** 系统返回 schoolId 与 campusId 均为 `null`

#### Scenario: 已设置时返回保存值

- **WHEN** 用户已设置学校
- **THEN** 系统返回 schoolId/schoolName 及（可选）campusId/campusName
