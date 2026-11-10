package com.lostfound.moderation;

import org.springframework.stereotype.Component;

import java.util.List;

/** 内容自动检测器。MVP 用确定性违禁词匹配，规则见 contracts/moderation-rules.yaml。 */
@Component
public class ContentModerator {

    // 必须与 contracts/moderation-rules.yaml 的 banned_keywords 保持一致。
    private static final List<String> BANNED_KEYWORDS = List.of("转账", "刷单", "诈骗");

    /** 对标题与公开描述做大小写不敏感包含匹配，命中任一违禁词返回 true。 */
    public boolean isRejected(String title, String description) {
        String t = title == null ? "" : title.toLowerCase();
        String d = description == null ? "" : description.toLowerCase();
        for (String keyword : BANNED_KEYWORDS) {
            if (t.contains(keyword) || d.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
