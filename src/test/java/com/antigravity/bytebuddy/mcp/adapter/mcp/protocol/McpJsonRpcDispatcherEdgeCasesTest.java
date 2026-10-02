package com.antigravity.bytebuddy.mcp.adapter.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpJsonRpcDispatcherEdgeCasesTest {

    private ObjectMapper mapper;
    private McpJsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        dispatcher = new McpJsonRpcDispatcher(mapper);

        // Register a dummy failing tool
        dispatcher.registerTool(new McpToolHandler() {
            @Override
            public McpToolDefinition getDefinition() {
                return new McpToolDefinition("failing_tool", "Tool that always fails", Map.of());
            }

            @Override
            public String execute(JsonNode arguments) throws Exception {
                throw new IllegalStateException("Simulated tool failure");
            }
        });
    }

    @Test
    @DisplayName("Calling unknown tool should return JSON-RPC error code -32601")
    void testUnknownToolCall() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "id": 100,
                    "method": "tools/call",
                    "params": {
                        "name": "non_existent_tool",
                        "arguments": {}
                    }
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        JsonNode resp = mapper.readTree(responseStr);

        assertThat(resp.has("error")).isTrue();
        assertThat(resp.path("error").path("code").asInt()).isEqualTo(-32601);
        assertThat(resp.path("error").path("message").asText()).contains("Tool not found: non_existent_tool");
    }

    @Test
    @DisplayName("Calling unsupported method with ID should return JSON-RPC error code -32601")
    void testUnsupportedMethodWithId() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "id": 101,
                    "method": "custom/unknown_action",
                    "params": {}
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        JsonNode resp = mapper.readTree(responseStr);

        assertThat(resp.has("error")).isTrue();
        assertThat(resp.path("error").path("code").asInt()).isEqualTo(-32601);
        assertThat(resp.path("error").path("message").asText()).contains("Method not supported: custom/unknown_action");
    }

    @Test
    @DisplayName("Calling unsupported method as notification (without ID) should return null")
    void testUnsupportedNotificationWithoutId() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "method": "custom/notification",
                    "params": {}
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        assertThat(responseStr).isNull();
    }

    @Test
    @DisplayName("notifications/initialized should return null")
    void testInitializedNotification() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "method": "notifications/initialized"
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        assertThat(responseStr).isNull();
    }

    @Test
    @DisplayName("ping method should return empty result object")
    void testPing() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "id": "ping-1",
                    "method": "ping"
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        JsonNode resp = mapper.readTree(responseStr);

        assertThat(resp.path("id").asText()).isEqualTo("ping-1");
        assertThat(resp.path("result").isObject()).isTrue();
    }

    @Test
    @DisplayName("Tool throwing exception should result in isError = true with error text")
    void testToolThrowingException() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "id": 200,
                    "method": "tools/call",
                    "params": {
                        "name": "failing_tool",
                        "arguments": {}
                    }
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        JsonNode resp = mapper.readTree(responseStr);

        assertThat(resp.path("result").path("isError").asBoolean()).isTrue();
        String text = resp.path("result").path("content").get(0).path("text").asText();
        assertThat(text).contains("Error: Simulated tool failure");
    }

    @Test
    @DisplayName("Message without method should return null")
    void testMessageWithoutMethod() throws Exception {
        String json = """
                {
                    "jsonrpc": "2.0",
                    "id": 5,
                    "result": {}
                }
                """;

        String responseStr = dispatcher.handleMessage(mapper.readTree(json));
        assertThat(responseStr).isNull();
    }

    @Test
    @DisplayName("stdio start loop should catch invalid JSON and output Parse error (-32700)")
    void testStdioMalformedJsonParsing() throws Exception {
        String input = "not a valid json string\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dispatcher.start(in, out);

        String output = out.toString(StandardCharsets.UTF_8).trim();
        assertThat(output).contains("\"code\":-32700");
        assertThat(output).contains("Parse error");
    }
}
