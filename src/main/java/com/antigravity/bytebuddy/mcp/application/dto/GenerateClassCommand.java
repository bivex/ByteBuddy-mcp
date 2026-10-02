package com.antigravity.bytebuddy.mcp.application.dto;

import com.antigravity.bytebuddy.mcp.domain.model.FieldDefinition;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;

import java.util.List;

public record GenerateClassCommand(
        String className,
        String superClassName,
        List<String> interfaces,
        List<FieldDefinition> fields,
        List<MethodDefinition> methods
) {
    public GenerateClassCommand {
        if (interfaces == null) interfaces = List.of();
        if (fields == null) fields = List.of();
        if (methods == null) methods = List.of();
    }
}
