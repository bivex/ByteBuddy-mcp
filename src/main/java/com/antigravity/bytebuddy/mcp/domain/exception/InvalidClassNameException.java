package com.antigravity.bytebuddy.mcp.domain.exception;

public class InvalidClassNameException extends DomainException {
    public InvalidClassNameException(String name, String reason) {
        super(String.format("Invalid class name '%s': %s", name, reason));
    }
}
