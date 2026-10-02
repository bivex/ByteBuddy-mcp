package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.InspectCommand;
import com.antigravity.bytebuddy.mcp.application.dto.InspectionResponse;
import com.antigravity.bytebuddy.mcp.domain.port.in.InspectBytecodeUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class InspectTool implements McpToolHandler {
    private final InspectBytecodeUseCase inspectBytecodeUseCase;
    private final ObjectMapper mapper;

    public InspectTool(InspectBytecodeUseCase inspectBytecodeUseCase, ObjectMapper mapper) {
        this.inspectBytecodeUseCase = inspectBytecodeUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_inspect",
                "Inspect the structure of a bytecode class: modifiers, superclass, interfaces, fields, and method signatures.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "className", Map.of("type", "string", "description", "Class name in current session"),
                                "bytecodeBase64", Map.of("type", "string", "description", "Raw Base64 encoded .class bytes (if not stored in session)")
                        )
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String className = arguments.has("className") ? arguments.path("className").asText() : null;
        String bytecodeBase64 = arguments.has("bytecodeBase64") ? arguments.path("bytecodeBase64").asText() : null;

        InspectCommand command = new InspectCommand(className, bytecodeBase64);
        InspectionResponse response = inspectBytecodeUseCase.inspect(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
