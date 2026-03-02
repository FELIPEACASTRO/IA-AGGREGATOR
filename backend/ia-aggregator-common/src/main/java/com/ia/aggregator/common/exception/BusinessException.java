package com.ia.aggregator.common.exception;

/**
 * Business rule violation (4xx).
 */
public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
