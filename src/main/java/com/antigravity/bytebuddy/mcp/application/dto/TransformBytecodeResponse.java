package com.antigravity.bytebuddy.mcp.application.dto;

import java.util.List;

public record TransformBytecodeResponse(
        String className,
        int originalSize,
        int transformedSize,
        List<String> appliedRules,
        String decompiledJava,
        String bytecodeBase64
) {
}
