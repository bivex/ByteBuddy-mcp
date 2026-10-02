package com.antigravity.bytebuddy.mcp.application.dto;

public record InspectCommand(
        String className,
        String bytecodeBase64
) {
}
