package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.TransformBytecodeCommand;
import com.antigravity.bytebuddy.mcp.application.dto.TransformBytecodeResponse;
import com.antigravity.bytebuddy.mcp.domain.model.InterceptionRule;
import com.antigravity.bytebuddy.mcp.domain.port.in.TransformBytecodeUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransformClassTool implements McpToolHandler {
    private final TransformBytecodeUseCase transformBytecodeUseCase;
    private final ObjectMapper mapper;

    public TransformClassTool(TransformBytecodeUseCase transformBytecodeUseCase, ObjectMapper mapper) {
        this.transformBytecodeUseCase = transformBytecodeUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_transform_class",
                "Transform class bytecode using ByteBuddy. Intercept methods via regex and weave advice (MEASURE_EXECUTION_TIME, LOG_CALL_AND_RETURN, OVERRIDE_RETURN_VALUE, SUPPRESS_EXCEPTIONS).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "className", Map.of("type", "string", "description", "Class name in current session or to identify bytecode"),
                                "bytecodeBase64", Map.of("type", "string", "description", "Optional raw .class bytecode in Base64"),
                                "rules", Map.of("type", "array", "items", Map.of("type", "object"), "description", "Rules: [{methodPattern: '.*calculate.*', adviceType: 'MEASURE_EXECUTION_TIME'}]")
                        ),
                        "required", List.of("rules")
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String className = arguments.has("className") ? arguments.path("className").asText() : null;
        String bytecodeBase64 = arguments.has("bytecodeBase64") ? arguments.path("bytecodeBase64").asText() : null;

        List<InterceptionRule> rules = new ArrayList<>();
        if (arguments.has("rules") && arguments.get("rules").isArray()) {
            for (JsonNode rNode : arguments.get("rules")) {
                String pattern = rNode.path("methodPattern").asText(".*");
                String adviceStr = rNode.path("adviceType").asText("MEASURE_EXECUTION_TIME");
                InterceptionRule.AdviceType adviceType = InterceptionRule.AdviceType.valueOf(adviceStr.toUpperCase());

                Object overrideVal = null;
                if (rNode.has("overrideValue")) {
                    JsonNode ov = rNode.get("overrideValue");
                    if (ov.isTextual()) overrideVal = ov.asText();
                    else if (ov.isNumber()) overrideVal = ov.numberValue();
                    else if (ov.isBoolean()) overrideVal = ov.asBoolean();
                }

                rules.add(new InterceptionRule(pattern, adviceType, overrideVal));
            }
        }

        TransformBytecodeCommand command = new TransformBytecodeCommand(className, bytecodeBase64, rules);
        TransformBytecodeResponse response = transformBytecodeUseCase.transform(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
