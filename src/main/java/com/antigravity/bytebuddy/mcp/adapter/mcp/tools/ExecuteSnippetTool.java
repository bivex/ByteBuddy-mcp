package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.ExecuteSnippetCommand;
import com.antigravity.bytebuddy.mcp.application.dto.SnippetExecutionResponse;
import com.antigravity.bytebuddy.mcp.domain.port.in.ExecuteSnippetUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class ExecuteSnippetTool implements McpToolHandler {
    private final ExecuteSnippetUseCase executeSnippetUseCase;
    private final ObjectMapper mapper;

    public ExecuteSnippetTool(ExecuteSnippetUseCase executeSnippetUseCase, ObjectMapper mapper) {
        this.executeSnippetUseCase = executeSnippetUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_execute_snippet",
                "Compile and execute raw Java code using the official ByteBuddy library (net.bytebuddy.*) directly. Supports full fluent API (e.g. 'return new ByteBuddy(ClassFileVersion.JAVA_V17).subclass(Object.class).name(\"MyType\").make();'), automatically registers any generated classes into the session, and returns the execution result.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sourceCode", Map.of("type", "string", "description", "Raw Java code snippet (statements or complete class definition) utilizing official ByteBuddy APIs"),
                                "className", Map.of("type", "string", "description", "Optional class name if a complete class definition is provided")
                        ),
                        "required", List.of("sourceCode")
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String sourceCode = arguments.path("sourceCode").asText();
        String className = arguments.has("className") ? arguments.path("className").asText() : null;

        ExecuteSnippetCommand command = new ExecuteSnippetCommand(sourceCode, className);
        SnippetExecutionResponse response = executeSnippetUseCase.executeSnippet(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
