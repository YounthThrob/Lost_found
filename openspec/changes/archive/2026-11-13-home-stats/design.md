# 设计：广场统计（home-stats）

## 接口

`GET /stats`（需登录，bearerAuth），返回统一包裹：

```json
{
  "data": { "foundCount": 0, "lostCount": 0, "completedToday": 0 },
  "requestId": "...",
  "timestamp": "..."
}
```

## 统计口径

- `foundCount`：`listVisible()`（PUBLISHED/VERIFYING）中 `type == FOUND` 的数量。
- `lostCount`：同源中 `type == LOST` 的数量。
- `completedToday`：全部信息中 `status == COMPLETED`，且 `updatedAt`（交接双方确认完成时写入）落在「今日（北京时间 UTC+8）」的数量。

## 今日边界

统一采用北京时间（Asia/Shanghai，恒为 UTC+8，无夏令时）：

- Java：`ZoneId.of("Asia/Shanghai")` + `LocalDate.now(zone)`，`Instant.parse(updatedAt).atZone(zone).toLocalDate()`。
- Python：`timezone(timedelta(hours=8))`，`datetime.fromisoformat(updatedAt.replace("Z", "+00:00")).astimezone(zone).date()`。

两端将 `updatedAt`（RFC3339 UTC）转换到该时区后取日期比对，保证口径一致。

## 契约与等价

- 新增 `StatsResponse` schema（openapi/product-api.yaml）。
- 无需新增错误码。
- 双后端对同一状态返回一致的 HTTP 状态码与字段。
- 契约测试新增 `stats` 向量，断言三类数量均为正且字段键一致（java == python == expected）。

## 隐私与非官方边界

统计仅返回聚合计数，SHALL NOT 暴露任何信息内容、私密核验特征、认领证据或自由消息；文案不暗示校方认证。
