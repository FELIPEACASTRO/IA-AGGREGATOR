package com.ia.aggregator.common.exception;

public class AiGatewayException extends TechnicalException {

    private final String requestId;
    private final String providerId;
    private final String model;
    private final boolean retryable;

    public AiGatewayException(ErrorCode errorCode,
                              String requestId,
                              String providerId,
                              String model,
                              boolean retryable,
                              String detail) {
        super(errorCode, detail);
        this.requestId = requestId;
        this.providerId = providerId;
        this.model = model;
        this.retryable = retryable;
    }

    public AiGatewayException(ErrorCode errorCode,
                              String requestId,
                              String providerId,
                              String model,
                              boolean retryable,
                              String detail,
                              Throwable cause) {
        super(errorCode, detail, cause);
        this.requestId = requestId;
        this.providerId = providerId;
        this.model = model;
        this.retryable = retryable;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getModel() {
        return model;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
