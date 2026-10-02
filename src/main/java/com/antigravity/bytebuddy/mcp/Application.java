package com.antigravity.bytebuddy.mcp;

import com.antigravity.bytebuddy.mcp.adapter.mcp.ByteBuddyMcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        log.info("=================================================");
        log.info("Starting ByteBuddy MCP Server (Hexagonal / DDD)");
        log.info("Listening on stdio (JSON-RPC 2.0)...");
        log.info("=================================================");

        ByteBuddyMcpServer server = new ByteBuddyMcpServer();
        server.run(System.in, System.out);
    }
}
