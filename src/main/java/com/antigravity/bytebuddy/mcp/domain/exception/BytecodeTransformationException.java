package com.antigravity.bytebuddy.mcp.domain.exception;

public class BytecodeTransformationException extends DomainException {
    public BytecodeTransformationException(String message) {
        super(message);
    }

    public BytecodeTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
