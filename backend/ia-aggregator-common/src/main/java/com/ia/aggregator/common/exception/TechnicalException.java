package com.ia.aggregator.common.exception;

/**
 * Technical/infrastructure failure (5xx).
 */
public class TechnicalException extends BaseException {

    public TechnicalException(ErrorCode errorCode) {
        super(errorCode, null);
    }

    public TechnicalException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public TechnicalException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
