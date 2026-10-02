package com.antigravity.bytebuddy.mcp.adapter.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public interface McpToolHandler {

    McpToolDefinition getDefinition();

    String execute(JsonNode arguments) throws Exception;
}
