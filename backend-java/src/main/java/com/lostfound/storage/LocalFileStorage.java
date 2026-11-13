package com.lostfound.storage;

import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件系统存储。写 {@code storage.local-dir}（默认 ./data/media）。
 * key 仅允许 {@code [0-9a-f-]}，防止路径穿越。
 */
@Component
public class LocalFileStorage implements Storage {

    private static final String KEY_PATTERN = "[0-9a-f-]+";

    private final Path baseDir;

    public LocalFileStorage(@Value("${storage.local-dir:./data/media}") String localDir) {
        this.baseDir = Paths.get(localDir).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public byte[] get(String key) {
        Path target = resolve(key);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        Path target = resolve(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 删除失败不影响元数据清理。
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches(KEY_PATTERN)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return baseDir.resolve(key).normalize();
    }
}
