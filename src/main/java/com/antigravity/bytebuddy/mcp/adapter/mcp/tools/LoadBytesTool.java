package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.ClassSummaryResponse;
import com.antigravity.bytebuddy.mcp.application.dto.LoadBytesCommand;
import com.antigravity.bytebuddy.mcp.domain.port.in.ClassSessionUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class LoadBytesTool implements McpToolHandler {
    private final ClassSessionUseCase sessionUseCase;
    private final ObjectMapper mapper;

    public LoadBytesTool(ClassSessionUseCase sessionUseCase, ObjectMapper mapper) {
        this.sessionUseCase = sessionUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_load_bytes",
                "Ingest raw Base64-encoded .class bytecode into the current session for disassembly, inspection, or transformation.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "className", Map.of("type", "string", "description", "Optional class name identifier"),
                                "bytecodeBase64", Map.of("type", "string", "description", "Raw Base64-encoded .class bytes")
                        ),
                        "required", List.of("bytecodeBase64")
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String className = arguments.has("className") ? arguments.path("className").asText() : null;
        String bytecodeBase64 = arguments.path("bytecodeBase64").asText();

        LoadBytesCommand command = new LoadBytesCommand(className, bytecodeBase64);
        ClassSummaryResponse response = sessionUseCase.loadClassBytes(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
