package com.antigravity.bytebuddy.mcp.application.dto;

public record ExecuteSnippetCommand(
        String sourceCode,
        String className
) {
}
