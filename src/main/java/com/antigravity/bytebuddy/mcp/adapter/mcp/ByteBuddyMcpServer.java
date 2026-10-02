package com.antigravity.bytebuddy.mcp.adapter.mcp;

import com.antigravity.bytebuddy.mcp.adapter.mcp.protocol.McpJsonRpcDispatcher;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.DisassembleTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.ExecuteMethodTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.GenerateClassTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.InspectTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.ListClassesTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.LoadBytesTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.LoadFromJarTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.TransformClassTool;
import com.antigravity.bytebuddy.mcp.application.service.ClassSessionService;
import com.antigravity.bytebuddy.mcp.application.service.DisassembleBytecodeService;
import com.antigravity.bytebuddy.mcp.application.service.ExecuteDynamicMethodService;
import com.antigravity.bytebuddy.mcp.application.service.GenerateClassService;
import com.antigravity.bytebuddy.mcp.application.service.InspectBytecodeService;
import com.antigravity.bytebuddy.mcp.application.service.TransformBytecodeService;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeEnginePort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.DynamicExecutionPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy.ByteBuddyEngineAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution.IsolatedDynamicExecutionAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Driving adapter that configures the Hexagonal domain, wires dependencies, and runs the MCP Server.
 */
public class ByteBuddyMcpServer {

    private final McpJsonRpcDispatcher dispatcher;

    public ByteBuddyMcpServer() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 1. Driven (Secondary) Adapters
        ClassRepositoryPort repositoryPort = new InMemoryClassRepositoryAdapter();
        BytecodeDisassemblerPort disassemblerPort = new AsmAndCfrDisassemblerAdapter();
        BytecodeEnginePort enginePort = new ByteBuddyEngineAdapter(disassemblerPort);
        DynamicExecutionPort executionPort = new IsolatedDynamicExecutionAdapter(repositoryPort);

        // 2. Application Services (Use Cases)
        GenerateClassService generateService = new GenerateClassService(enginePort, disassemblerPort, repositoryPort);
        TransformBytecodeService transformService = new TransformBytecodeService(enginePort, disassemblerPort, repositoryPort);
        DisassembleBytecodeService disassembleService = new DisassembleBytecodeService(disassemblerPort, repositoryPort);
        InspectBytecodeService inspectService = new InspectBytecodeService(disassemblerPort, repositoryPort);
        ExecuteDynamicMethodService executionService = new ExecuteDynamicMethodService(executionPort, repositoryPort);
        ClassSessionService sessionService = new ClassSessionService(repositoryPort, disassemblerPort);

        // 3. Dispatcher and Tool Registration
        this.dispatcher = new McpJsonRpcDispatcher(mapper);
        dispatcher.registerTool(new GenerateClassTool(generateService, mapper));
        dispatcher.registerTool(new TransformClassTool(transformService, mapper));
        dispatcher.registerTool(new DisassembleTool(disassembleService, mapper));
        dispatcher.registerTool(new InspectTool(inspectService, mapper));
        dispatcher.registerTool(new ExecuteMethodTool(executionService, mapper));
        dispatcher.registerTool(new ListClassesTool(sessionService, mapper));
        dispatcher.registerTool(new LoadBytesTool(sessionService, mapper));
        dispatcher.registerTool(new LoadFromJarTool(sessionService, mapper));
    }

    public void run(InputStream in, OutputStream out) {
        dispatcher.start(in, out);
    }
}
