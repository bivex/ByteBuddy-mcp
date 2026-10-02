package com.antigravity.bytebuddy.mcp.application.dto;

public record DisassembleCommand(
        String className,
        String bytecodeBase64,
        boolean includeDecompiledJava,
        boolean includeOpcodes
) {
    public DisassembleCommand(String className, String bytecodeBase64) {
        this(className, bytecodeBase64, true, true);
    }
}
