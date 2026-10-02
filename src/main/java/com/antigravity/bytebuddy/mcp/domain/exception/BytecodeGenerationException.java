package com.antigravity.bytebuddy.mcp.domain.exception;

public class BytecodeGenerationException extends DomainException {
    public BytecodeGenerationException(String message) {
        super(message);
    }

    public BytecodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
