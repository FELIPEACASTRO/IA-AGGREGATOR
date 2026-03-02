package com.ia.aggregator.common.exception;

/**
 * Base exception for all platform exceptions.
 */
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    protected BaseException(ErrorCode errorCode, String detail) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    protected BaseException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String getDetail() { return detail; }
}
