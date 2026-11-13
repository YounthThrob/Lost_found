package com.lostfound.directory;

import java.util.List;

/** 学校目录实体。主键 UUIDv7 或等价不可预测标识；别名归一。 */
public record School(String id, String name, List<String> aliases, String source,
                     boolean enabled, String createdAt) {
}
