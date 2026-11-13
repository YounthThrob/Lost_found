package com.lostfound.storage;

import com.lostfound.shared.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void put_then_get_roundtrip() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        storage.put("abc123", new byte[]{1, 2, 3}, "image/jpeg");
        assertArrayEquals(new byte[]{1, 2, 3}, storage.get("abc123"));
    }

    @Test
    void delete_removes_content() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        storage.put("abc123", new byte[]{1}, "image/jpeg");
        storage.delete("abc123");
        assertNull(storage.get("abc123"));
    }

    @Test
    void invalid_key_is_rejected() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString());
        assertThrows(BusinessException.class,
            () -> storage.put("../evil", new byte[]{1}, "image/jpeg"));
        assertThrows(BusinessException.class,
            () -> storage.put("has space", new byte[]{1}, "image/jpeg"));
    }
}
