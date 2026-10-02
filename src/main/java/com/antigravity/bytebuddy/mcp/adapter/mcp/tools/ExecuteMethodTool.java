package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.ExecuteMethodCommand;
import com.antigravity.bytebuddy.mcp.application.dto.ExecutionResponse;
import com.antigravity.bytebuddy.mcp.domain.port.in.ExecuteDynamicMethodUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecuteMethodTool implements McpToolHandler {
    private final ExecuteDynamicMethodUseCase executeDynamicMethodUseCase;
    private final ObjectMapper mapper;

    public ExecuteMethodTool(ExecuteDynamicMethodUseCase executeDynamicMethodUseCase, ObjectMapper mapper) {
        this.executeDynamicMethodUseCase = executeDynamicMethodUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_execute_method",
                "Load a generated or transformed class into an isolated ClassLoader, instantiate it, and execute a specified method with arguments.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "className", Map.of("type", "string", "description", "Class name in current session"),
                                "methodName", Map.of("type", "string", "description", "Method name to invoke"),
                                "arguments", Map.of("type", "array", "items", Map.of("type", "object"), "description", "Method argument values (primitives or strings)")
                        ),
                        "required", List.of("className", "methodName")
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String className = arguments.path("className").asText();
        String methodName = arguments.path("methodName").asText();

        List<Object> argsList = new ArrayList<>();
        if (arguments.has("arguments") && arguments.get("arguments").isArray()) {
            for (JsonNode argNode : arguments.get("arguments")) {
                if (argNode.isTextual()) argsList.add(argNode.asText());
                else if (argNode.isInt()) argsList.add(argNode.asInt());
                else if (argNode.isLong()) argsList.add(argNode.asLong());
                else if (argNode.isDouble()) argsList.add(argNode.asDouble());
                else if (argNode.isBoolean()) argsList.add(argNode.asBoolean());
                else if (argNode.isNull()) argsList.add(null);
                else argsList.add(argNode.toString());
            }
        }

        ExecuteMethodCommand command = new ExecuteMethodCommand(className, methodName, argsList);
        ExecutionResponse response = executeDynamicMethodUseCase.executeMethod(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
