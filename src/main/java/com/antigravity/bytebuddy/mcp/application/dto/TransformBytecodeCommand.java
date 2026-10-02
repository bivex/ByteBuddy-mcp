package com.antigravity.bytebuddy.mcp.application.dto;

import com.antigravity.bytebuddy.mcp.domain.model.InterceptionRule;

import java.util.List;

public record TransformBytecodeCommand(
        String className,
        String bytecodeBase64,
        List<InterceptionRule> rules
) {
    public TransformBytecodeCommand {
        if (rules == null) rules = List.of();
    }
}
