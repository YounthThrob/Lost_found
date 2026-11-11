package com.lostfound.shared;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Envelope.ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.code().httpStatus()).body(Envelope.error(e.code()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Envelope.ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
            .body(Envelope.error(ErrorCode.VALIDATION_FAILED));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Envelope.ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.httpStatus())
            .body(Envelope.error(ErrorCode.VALIDATION_FAILED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Envelope.ErrorResponse> handleUnexpected(Exception e) {
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
            .body(Envelope.error(ErrorCode.INTERNAL_ERROR));
    }
}
