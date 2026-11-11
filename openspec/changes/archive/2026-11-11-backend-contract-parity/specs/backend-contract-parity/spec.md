# Spec Delta

## Purpose

将 PRODUCT.md §12.4 的等价性验收落实为可执行的契约要求：Java 与 Python 两个后端对同一请求返回一致的状态码、业务错误码、字段名、分页格式与时间格式，并以共同黑盒契约测试作为验收门槛。数据库迁移等价与 OpenAPI breaking-change 工具属后续 change。

## ADDED Requirements

### Requirement: 统一响应包裹

系统 SHALL 对所有成功与错误响应使用统一包裹结构：成功为 `{data, requestId, timestamp}`，错误为 `{error: {code, message, details}, requestId, timestamp}`。`timestamp` SHALL 采用 RFC3339 UTC（以 `Z` 结尾）。

#### Scenario: 成功响应结构

- **WHEN** 任一接口成功返回
- **THEN** 响应 SHALL 包含 `data`、`requestId`、`timestamp` 三个字段，且 `timestamp` 为 RFC3339 UTC

#### Scenario: 错误响应结构

- **WHEN** 任一接口返回错误
- **THEN** 响应 SHALL 包含 `error.code`、`error.message`、`error.details` 与 `requestId`、`timestamp`，且 `error.details` 为空对象

### Requirement: 分页契约

系统 SHALL 对列表接口使用统一分页格式 `{items, pagination: {page, pageSize, total, hasMore}}`。`page` 从 1 起、默认 1；`pageSize` 默认 20、上限 100。`page < 1` 或 `pageSize > 100` 时 SHALL 返回 `VALIDATION_FAILED`（HTTP 422）。

#### Scenario: 分页结构一致

- **WHEN** 客户端请求任一列表接口
- **THEN** 响应 SHALL 返回 `items` 与 `pagination`，且 `pagination` 含 `page`、`pageSize`、`total`、`hasMore` 四个字段

#### Scenario: 分页边界校验

- **WHEN** 客户端以 `page=0` 或 `pageSize=101` 请求列表接口
- **THEN** 系统 SHALL 返回 `VALIDATION_FAILED`（HTTP 422）

#### Scenario: 越界页码

- **WHEN** 客户端请求超出最后一页的页码
- **THEN** 系统 SHALL 返回空 `items` 且 `hasMore=false`，`total` 保持不变

### Requirement: 请求参数绑定错误一致性

两个后端实现 SHALL 对非法请求参数（如非整数的 `page`）返回相同错误码 `VALIDATION_FAILED`（HTTP 422），不泄露框架内部错误。

#### Scenario: 非整数分页参数

- **WHEN** 客户端以非整数值（如 `page=abc`）请求列表接口
- **THEN** 系统 SHALL 返回 `VALIDATION_FAILED`（HTTP 422）

### Requirement: 黑盒契约测试作为等价性验收

Java 与 Python 两个实现 SHALL 通过同一组黑盒契约测试（`contracts/run_contract_tests.py`），对相同请求产生一致的状态码、业务错误码、字段名、分页格式与时间格式。

#### Scenario: 契约测试通过

- **WHEN** 对两后端运行共同契约测试
- **THEN** 两后端 SHALL 对每个探针返回与共同期望一致的结果，且彼此一致（`java == python == expected`）
