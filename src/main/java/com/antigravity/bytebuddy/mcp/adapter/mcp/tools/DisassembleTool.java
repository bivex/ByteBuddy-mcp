package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolDefinition;
import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpToolHandler;
import com.antigravity.bytebuddy.mcp.application.dto.DisassembleCommand;
import com.antigravity.bytebuddy.mcp.application.dto.DisassemblyResponse;
import com.antigravity.bytebuddy.mcp.domain.port.in.DisassembleBytecodeUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class DisassembleTool implements McpToolHandler {
    private final DisassembleBytecodeUseCase disassembleBytecodeUseCase;
    private final ObjectMapper mapper;

    public DisassembleTool(DisassembleBytecodeUseCase disassembleBytecodeUseCase, ObjectMapper mapper) {
        this.disassembleBytecodeUseCase = disassembleBytecodeUseCase;
        this.mapper = mapper;
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(
                "bytebuddy_disassemble",
                "Disassemble Java bytecode into readable JVM assembly opcodes (via OW2 ASM) and decompile into Java source code (via CFR).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "className", Map.of("type", "string", "description", "Class name in current session"),
                                "bytecodeBase64", Map.of("type", "string", "description", "Raw Base64 encoded .class bytes (if not stored in session)"),
                                "includeDecompiledJava", Map.of("type", "boolean", "description", "Include CFR decompiled Java source (default true)"),
                                "includeOpcodes", Map.of("type", "boolean", "description", "Include ASM bytecode opcode listing (default true)")
                        )
                )
        );
    }

    @Override
    public String execute(JsonNode arguments) throws Exception {
        String className = arguments.has("className") ? arguments.path("className").asText() : null;
        String bytecodeBase64 = arguments.has("bytecodeBase64") ? arguments.path("bytecodeBase64").asText() : null;
        boolean includeDecompiled = !arguments.has("includeDecompiledJava") || arguments.path("includeDecompiledJava").asBoolean(true);
        boolean includeOpcodes = !arguments.has("includeOpcodes") || arguments.path("includeOpcodes").asBoolean(true);

        DisassembleCommand command = new DisassembleCommand(className, bytecodeBase64, includeDecompiled, includeOpcodes);
        DisassemblyResponse response = disassembleBytecodeUseCase.disassemble(command);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }
}
