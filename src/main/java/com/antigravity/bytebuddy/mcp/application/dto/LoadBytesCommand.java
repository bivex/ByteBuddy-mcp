package com.antigravity.bytebuddy.mcp.application.dto;

public record LoadBytesCommand(
        String className,
        String bytecodeBase64
) {
}
