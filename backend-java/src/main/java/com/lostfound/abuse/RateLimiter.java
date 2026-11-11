package com.lostfound.abuse;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** 固定窗口频率限制器。按用户维度、按操作独立计数，阈值见 contracts/rate-limits.yaml。 */
@Component
public class RateLimiter {

    // 必须与 contracts/rate-limits.yaml 保持一致。
    private static final long WINDOW_SECONDS = 60;
    private static final Map<String, Integer> LIMITS = Map.of(
        "claim", 20,
        "post", 20,
        "message", 30,
        "report", 20);

    private static final class Counter {
        long window;
        int count;
    }

    private final Map<String, Counter> counters = new HashMap<>();

    /** 对指定用户与操作做一次限流计数；达到上限即抛出对应限流错误码。 */
    public void check(String userId, String operation) {
        Integer limit = LIMITS.get(operation);
        if (limit == null) {
            return;
        }
        long window = System.currentTimeMillis() / 1000 / WINDOW_SECONDS;
        String key = userId + ":" + operation;
        synchronized (this) {
            Counter c = counters.get(key);
            if (c == null || c.window != window) {
                c = new Counter();
                c.window = window;
                counters.put(key, c);
            }
            if (c.count >= limit) {
                throw new BusinessException(
                    "claim".equals(operation) ? ErrorCode.CLAIM_RATE_LIMITED : ErrorCode.RATE_LIMITED);
            }
            c.count++;
        }
    }

    /** 仅供测试重置状态。 */
    public void clear() {
        synchronized (this) {
            counters.clear();
        }
    }
}
