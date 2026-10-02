package com.antigravity.bytebuddy.mcp.domain.exception;

public class DynamicExecutionException extends DomainException {
    public DynamicExecutionException(String message) {
        super(message);
    }

    public DynamicExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
