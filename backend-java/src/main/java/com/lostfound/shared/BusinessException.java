package com.lostfound.shared;

/** 携带业务错误码的运行时异常，由 GlobalExceptionHandler 统一转为错误响应。 */
public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(ErrorCode code) {
        super(code.message());
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
