package com.antigravity.bytebuddy.mcp.adapter.mcp.protocol;

import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.ExecuteMethodTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.GenerateClassTool;
import com.antigravity.bytebuddy.mcp.adapter.mcp.tools.ListClassesTool;
import com.antigravity.bytebuddy.mcp.application.service.ClassSessionService;
import com.antigravity.bytebuddy.mcp.application.service.ExecuteDynamicMethodService;
import com.antigravity.bytebuddy.mcp.application.service.GenerateClassService;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeEnginePort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.DynamicExecutionPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy.ByteBuddyEngineAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution.IsolatedDynamicExecutionAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpJsonRpcDispatcherTest {

    private ObjectMapper mapper;
    private McpJsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        dispatcher = new McpJsonRpcDispatcher(mapper);

        ClassRepositoryPort repo = new InMemoryClassRepositoryAdapter();
        BytecodeDisassemblerPort disasm = new AsmAndCfrDisassemblerAdapter();
        BytecodeEnginePort engine = new ByteBuddyEngineAdapter(disasm);
        DynamicExecutionPort exec = new IsolatedDynamicExecutionAdapter();

        GenerateClassService genService = new GenerateClassService(engine, disasm, repo);
        ExecuteDynamicMethodService execService = new ExecuteDynamicMethodService(exec, repo);
        ClassSessionService sessionService = new ClassSessionService(repo, disasm);

        dispatcher.registerTool(new GenerateClassTool(genService, mapper));
        dispatcher.registerTool(new ExecuteMethodTool(execService, mapper));
        dispatcher.registerTool(new ListClassesTool(sessionService, mapper));
    }

    @Test
    @DisplayName("MCP initialize should return protocolVersion 2024-11-05 and serverInfo")
    void testInitialize() throws Exception {
        JsonNode req = mapper.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        String response = dispatcher.handleMessage(req);
        assertThat(response).isNotNull();

        JsonNode res = mapper.readTree(response);
        assertThat(res.path("result").path("protocolVersion").asText()).isEqualTo("2024-11-05");
        assertThat(res.path("result").path("serverInfo").path("name").asText()).isEqualTo("bytebuddy-mcp-server");
    }

    @Test
    @DisplayName("MCP tools/list should list registered tools")
    void testToolsList() throws Exception {
        JsonNode req = mapper.readTree("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
        String response = dispatcher.handleMessage(req);

        JsonNode res = mapper.readTree(response);
        JsonNode tools = res.path("result").path("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("MCP tools/call for bytebuddy_generate_class and bytebuddy_execute_method end-to-end")
    void testGenerateAndCallDynamicClassViaMcp() throws Exception {
        // 1. Generate class
        String genJson = """
                {
                  "jsonrpc": "2.0",
                  "id": 10,
                  "method": "tools/call",
                  "params": {
                    "name": "bytebuddy_generate_class",
                    "arguments": {
                      "className": "com.test.DynamicMcpGreeter",
                      "methods": [
                        {
                          "name": "sayHi",
                          "returnType": "String",
                          "strategy": "RETURN_CONSTANT",
                          "constantValue": "Hi from MCP tool!"
                        }
                      ]
                    }
                  }
                }
                """;
        String genRespStr = dispatcher.handleMessage(mapper.readTree(genJson));
        JsonNode genResp = mapper.readTree(genRespStr);
        assertThat(genResp.path("result").path("isError").asBoolean()).isFalse();

        // 2. Execute method on generated class
        String execJson = """
                {
                  "jsonrpc": "2.0",
                  "id": 11,
                  "method": "tools/call",
                  "params": {
                    "name": "bytebuddy_execute_method",
                    "arguments": {
                      "className": "com.test.DynamicMcpGreeter",
                      "methodName": "sayHi"
                    }
                  }
                }
                """;
        String execRespStr = dispatcher.handleMessage(mapper.readTree(execJson));
        JsonNode execResp = mapper.readTree(execRespStr);
        assertThat(execResp.path("result").path("isError").asBoolean()).isFalse();

        String contentText = execResp.path("result").path("content").get(0).path("text").asText();
        assertThat(contentText).contains("Hi from MCP tool!");
    }
}
