package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.ClassSummaryResponse;
import com.antigravity.bytebuddy.mcp.domain.port.in.ClassSessionUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class ListClassesTool implements McpToolHandler {
    private final ClassSessionUseCase sessionUseCase;
    private final ObjectMapper mapper;

    public ListClassesTool(ClassSessionUseCase sessionUseCase, ObjectMapper mapper) {
        this.sessionUseCase = sessionUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_list_classes",
                "List all classes currently created, modified, or loaded in the active MCP session.",
                Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        List<ClassSummaryResponse> list = sessionUseCase.listClasses();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    }
}
