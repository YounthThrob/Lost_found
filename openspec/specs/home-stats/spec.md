# home-stats Specification

## Purpose
为首页提供动态的广场统计：招领/寻物公开数量与「今日找到方向」数量，数量由实际发布与交接完成驱动，Java 与 Python 双后端对同一请求返回一致结果。

## Requirements

### Requirement: 查询广场统计

已登录用户 SHALL 能够查询广场统计。系统 SHALL 返回 `foundCount`、`lostCount` 与 `completedToday` 三个字段；`foundCount` 与 `lostCount` SHALL 分别统计状态为 `PUBLISHED` 或 `VERIFYING` 的招领（FOUND）与寻物（LOST）信息数量；`completedToday` SHALL 统计状态为 `COMPLETED` 且完成时间落在当日（北京时间 UTC+8）的信息数量。

#### Scenario: 返回三类统计

- **WHEN** 已登录用户查询广场统计
- **THEN** 系统返回 200，data 包含 `foundCount`、`lostCount`、`completedToday`

#### Scenario: 招领与寻物仅统计公开信息

- **WHEN** 存在草稿、已关闭或已下架的招领/寻物信息
- **THEN** 这些信息 SHALL NOT 计入 `foundCount` 或 `lostCount`

#### Scenario: 今日完成交接计入 completedToday

- **WHEN** 有信息在当日（北京时间 UTC+8）完成交接（状态为 `COMPLETED`）
- **THEN** 该信息计入 `completedToday`

#### Scenario: 非当日完成交接不计入

- **WHEN** 信息完成交接的时间不在当日（北京时间 UTC+8）
- **THEN** 该信息 SHALL NOT 计入 `completedToday`
