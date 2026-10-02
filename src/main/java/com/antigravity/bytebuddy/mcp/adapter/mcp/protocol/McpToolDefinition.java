package com.antigravity.bytebuddy.mcp.adapter.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public record McpToolDefinition(
        String name,
        String description,
        Map<String, Object> inputSchema
) {
}
