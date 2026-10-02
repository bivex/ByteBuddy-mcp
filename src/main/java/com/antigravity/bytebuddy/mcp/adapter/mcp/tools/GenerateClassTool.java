package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.GenerateClassCommand;
import com.antigravity.bytebuddy.mcp.application.dto.GeneratedClassResponse;
import com.antigravity.bytebuddy.mcp.domain.model.FieldDefinition;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;
import com.antigravity.bytebuddy.mcp.domain.port.in.GenerateClassUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenerateClassTool implements McpToolHandler {
    private final GenerateClassUseCase generateClassUseCase;
    private final ObjectMapper mapper;

    public GenerateClassTool(GenerateClassUseCase generateClassUseCase, ObjectMapper mapper) {
        this.generateClassUseCase = generateClassUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_generate_class",
                "Dynamically synthesize a Java class using ByteBuddy. Define class name, optional superclass, interfaces, fields, and methods with strategies (RETURN_CONSTANT, THROW_EXCEPTION, CALL_SUPER, RETURN_DEFAULT).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "className", Map.of("type", "string", "description", "Fully qualified class name (e.g. com.example.Greeter)"),
                                "superClassName", Map.of("type", "string", "description", "Optional superclass name, defaults to java.lang.Object"),
                                "interfaces", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Optional list of fully qualified interface names"),
                                "fields", Map.of("type", "array", "items", Map.of("type", "object"), "description", "Fields: [{name: 'count', type: 'int'}]"),
                                "methods", Map.of("type", "array", "items", Map.of("type", "object"), "description", "Methods: [{name: 'greet', returnType: 'String', strategy: 'RETURN_CONSTANT', constantValue: 'Hello World'}]")
                        ),
                        "required", List.of("className")
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String className = arguments.path("className").asText();
        String superClass = arguments.has("superClassName") ? arguments.path("superClassName").asText() : null;

        List<String> interfaces = new ArrayList<>();
        if (arguments.has("interfaces") && arguments.get("interfaces").isArray()) {
            for (JsonNode iface : arguments.get("interfaces")) {
                interfaces.add(iface.asText());
            }
        }

        List<FieldDefinition> fields = new ArrayList<>();
        if (arguments.has("fields") && arguments.get("fields").isArray()) {
            for (JsonNode fNode : arguments.get("fields")) {
                String fName = fNode.path("name").asText();
                String fType = fNode.path("type").asText("Object");
                fields.add(FieldDefinition.of(fName, fType));
            }
        }

        List<MethodDefinition> methods = new ArrayList<>();
        if (arguments.has("methods") && arguments.get("methods").isArray()) {
            for (JsonNode mNode : arguments.get("methods")) {
                String mName = mNode.path("name").asText();
                String mReturnType = mNode.path("returnType").asText("void");
                String strategyStr = mNode.path("strategy").asText("RETURN_DEFAULT");
                MethodDefinition.Strategy strategy = MethodDefinition.Strategy.valueOf(strategyStr.toUpperCase());

                Object constantVal = null;
                if (mNode.has("constantValue")) {
                    JsonNode cv = mNode.get("constantValue");
                    if (cv.isTextual()) constantVal = cv.asText();
                    else if (cv.isNumber()) constantVal = cv.numberValue();
                    else if (cv.isBoolean()) constantVal = cv.asBoolean();
                }

                List<String> paramTypes = new ArrayList<>();
                if (mNode.has("parameterTypes") && mNode.get("parameterTypes").isArray()) {
                    for (JsonNode p : mNode.get("parameterTypes")) {
                        paramTypes.add(p.asText());
                    }
                }

                methods.add(new MethodDefinition(
                        mName,
                        mReturnType,
                        paramTypes,
                        Modifier.PUBLIC,
                        strategy,
                        constantVal
                ));
            }
        }

        GenerateClassCommand command = new GenerateClassCommand(className, superClass, interfaces, fields, methods);
        GeneratedClassResponse response = generateClassUseCase.generateClass(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
