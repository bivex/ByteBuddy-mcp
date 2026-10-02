package com.antigravity.bytebuddy.mcp.adapter.mcp.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles JSON-RPC 2.0 dispatching over stdio conforming to the Model Context Protocol.
 */
public class McpJsonRpcDispatcher {
    private static final Logger log = LoggerFactory.getLogger(McpJsonRpcDispatcher.class);
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

    private final ObjectMapper mapper;
    private final Map<String, McpToolHandler> tools = new HashMap<>();

    public McpJsonRpcDispatcher(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void registerTool(McpToolHandler toolHandler) {
        tools.put(toolHandler.getDefinition().name(), toolHandler);
        log.info("Registered MCP tool: {}", toolHandler.getDefinition().name());
    }

    public void start(InputStream in, OutputStream out) {
        log.info("Starting MCP stdio listener loop...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                log.debug("Received MCP JSON-RPC: {}", line);
                try {
                    JsonNode request = mapper.readTree(line);
                    String response = handleMessage(request);
                    if (response != null) {
                        log.debug("Sending MCP JSON-RPC response: {}", response);
                        writer.println(response);
                        writer.flush();
                    }
                } catch (Exception e) {
                    log.error("Error processing line: {}", line, e);
                    writer.println(createJsonRpcError(null, -32700, "Parse error: " + e.getMessage()));
                    writer.flush();
                }
            }
        } catch (Exception e) {
            log.info("MCP stdio connection terminated: {}", e.getMessage());
        }
    }

    public String handleMessage(JsonNode request) throws JsonProcessingException {
        if (!request.has("method")) {
            return null; // Might be response
        }

        String method = request.path("method").asText();
        JsonNode id = request.get("id");
        JsonNode params = request.path("params");

        switch (method) {
            case "initialize" -> {
                ObjectNode result = mapper.createObjectNode();
                result.put("protocolVersion", MCP_PROTOCOL_VERSION);

                ObjectNode capabilities = result.putObject("capabilities");
                capabilities.putObject("tools");

                ObjectNode serverInfo = result.putObject("serverInfo");
                serverInfo.put("name", "bytebuddy-mcp-server");
                serverInfo.put("version", "1.0.0");

                return createSuccessResponse(id, result);
            }

            case "notifications/initialized" -> {
                log.info("Client acknowledged initialized notification");
                return null; // Notifications do not have responses
            }

            case "ping" -> {
                return createSuccessResponse(id, mapper.createObjectNode());
            }

            case "tools/list" -> {
                ObjectNode result = mapper.createObjectNode();
                ArrayNode toolsArray = result.putArray("tools");

                for (McpToolHandler tool : tools.values()) {
                    McpToolDefinition def = tool.getDefinition();
                    ObjectNode toolNode = toolsArray.addObject();
                    toolNode.put("name", def.name());
                    toolNode.put("description", def.description());
                    toolNode.set("inputSchema", mapper.valueToTree(def.inputSchema()));
                }

                return createSuccessResponse(id, result);
            }

            case "tools/call" -> {
                String toolName = params.path("name").asText();
                JsonNode arguments = params.path("arguments");
                McpToolHandler handler = tools.get(toolName);

                if (handler == null) {
                    return createJsonRpcError(id, -32601, "Tool not found: " + toolName);
                }

                try {
                    String output = handler.execute(arguments);
                    ObjectNode result = mapper.createObjectNode();
                    ArrayNode contentArray = result.putArray("content");
                    ObjectNode textContent = contentArray.addObject();
                    textContent.put("type", "text");
                    textContent.put("text", output);
                    result.put("isError", false);

                    return createSuccessResponse(id, result);
                } catch (Exception e) {
                    log.error("Execution failed for tool '{}'", toolName, e);
                    ObjectNode result = mapper.createObjectNode();
                    ArrayNode contentArray = result.putArray("content");
                    ObjectNode textContent = contentArray.addObject();
                    textContent.put("type", "text");
                    textContent.put("text", "Error: " + e.getMessage());
                    result.put("isError", true);

                    return createSuccessResponse(id, result);
                }
            }

            default -> {
                log.warn("Unknown MCP method requested: {}", method);
                if (id != null && !id.isNull()) {
                    return createJsonRpcError(id, -32601, "Method not supported: " + method);
                }
                return null;
            }
        }
    }

    private String createSuccessResponse(JsonNode id, JsonNode result) throws JsonProcessingException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        }
        response.set("result", result);
        return mapper.writeValueAsString(response);
    }

    private String createJsonRpcError(JsonNode id, int code, String message) {
        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            if (id != null) {
                response.set("id", id);
            } else {
                response.putNull("id");
            }
            ObjectNode error = response.putObject("error");
            error.put("code", code);
            error.put("message", message);
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }
}
