package com.antigravity.bytebuddy.mcp.domain.exception;

public class ClassNotFoundDomainException extends DomainException {
    public ClassNotFoundDomainException(String className) {
        super(String.format("Class '%s' not found in repository or session", className));
    }
}
