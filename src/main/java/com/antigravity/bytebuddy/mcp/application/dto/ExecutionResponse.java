package com.antigravity.bytebuddy.mcp.application.dto;

public record ExecutionResponse(
        boolean success,
        Object returnValue,
        long executionDurationMs,
        String capturedOutput,
        String errorMessage,
        String exceptionClassName
) {
}
