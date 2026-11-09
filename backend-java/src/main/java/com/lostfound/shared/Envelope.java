package com.lostfound.shared;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** 统一响应包裹（PRODUCT.md §11.1）。 */
public final class Envelope {

    private Envelope() {
    }

    public record Response<T>(T data, String requestId, String timestamp) {
    }

    public record ErrorBody(String code, String message, Map<String, Object> details) {
    }

    public record ErrorResponse(ErrorBody error, String requestId, String timestamp) {
    }

    public static <T> Response<T> ok(T data) {
        return new Response<>(data, UUID.randomUUID().toString(), Instant.now().toString());
    }

    public static ErrorResponse error(ErrorCode code) {
        return new ErrorResponse(
            new ErrorBody(code.name(), code.message(), Map.of()),
            UUID.randomUUID().toString(),
            Instant.now().toString());
    }
}
