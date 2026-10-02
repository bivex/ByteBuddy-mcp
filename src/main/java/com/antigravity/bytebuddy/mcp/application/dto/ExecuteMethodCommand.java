package com.antigravity.bytebuddy.mcp.application.dto;

import java.util.List;

public record ExecuteMethodCommand(
        String className,
        String methodName,
        List<Object> arguments
) {
    public ExecuteMethodCommand {
        if (arguments == null) arguments = List.of();
    }
}
