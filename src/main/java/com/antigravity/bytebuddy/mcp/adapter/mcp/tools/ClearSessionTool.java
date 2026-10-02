package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.domain.port.in.ClassSessionUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ClearSessionTool implements McpToolHandler {
    private final ClassSessionUseCase sessionUseCase;
    private final ObjectMapper mapper;

    public ClearSessionTool(ClassSessionUseCase sessionUseCase, ObjectMapper mapper) {
        this.sessionUseCase = sessionUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_clear_session",
                "Explicitly clear all loaded and generated classes from the active session repository to immediately release heap memory.",
                Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        sessionUseCase.clearSession();
        System.gc(); // Suggest immediate garbage collection
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                "status", "SUCCESS",
                "message", "Session cleared. All bytecode and ClassLoader references released."
        ));
    }
}
