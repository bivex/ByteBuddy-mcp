package com.antigravity.bytebuddy.mcp.application.dto;

public record DisassemblyResponse(
        String className,
        int bytecodeSize,
        String decompiledJava,
        String asmOpcodes
) {
}
