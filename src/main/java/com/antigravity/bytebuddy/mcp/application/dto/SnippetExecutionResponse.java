package com.antigravity.bytebuddy.mcp.application.dto;

import java.util.List;

public record SnippetExecutionResponse(
        boolean success,
        Object returnValue,
        long executionDurationMs,
        String capturedOutput,
        List<String> registeredClasses,
        String errorMessage
) {
}
