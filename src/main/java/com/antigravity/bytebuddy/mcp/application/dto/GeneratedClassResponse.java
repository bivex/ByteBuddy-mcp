package com.antigravity.bytebuddy.mcp.application.dto;

import java.util.List;

public record GeneratedClassResponse(
        String className,
        int bytecodeSize,
        int majorVersion,
        String superClassName,
        List<String> interfaces,
        List<String> fields,
        List<String> methods,
        String decompiledJava,
        String bytecodeBase64
) {
}
