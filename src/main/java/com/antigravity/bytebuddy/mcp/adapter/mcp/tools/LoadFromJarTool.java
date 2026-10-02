package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.ClassSummaryResponse;
import com.antigravity.bytebuddy.mcp.domain.port.in.ClassSessionUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class LoadFromJarTool implements McpToolHandler {
    private final ClassSessionUseCase sessionUseCase;
    private final ObjectMapper mapper;

    public LoadFromJarTool(ClassSessionUseCase sessionUseCase, ObjectMapper mapper) {
        this.sessionUseCase = sessionUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_load_from_jar",
                "Load a class bytecode directly from a local JAR archive by specifying jarPath and className, or list all classes in the JAR if className is omitted.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "jarPath", Map.of("type", "string", "description", "Absolute path to the .jar file on the local filesystem"),
                                "className", Map.of("type", "string", "description", "Optional fully qualified class name to load into the session (e.g. org.apache.commons.codec.Charsets)")
                        ),
                        "required", List.of("jarPath")
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String jarPath = arguments.path("jarPath").asText();
        String className = arguments.has("className") ? arguments.path("className").asText() : null;

        if (className == null || className.isBlank()) {
            List<String> classes = sessionUseCase.listJarEntries(jarPath);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "jarPath", jarPath,
                    "totalClasses", classes.size(),
                    "classes", classes
            ));
        }

        ClassSummaryResponse response = sessionUseCase.loadFromJar(jarPath, className);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
