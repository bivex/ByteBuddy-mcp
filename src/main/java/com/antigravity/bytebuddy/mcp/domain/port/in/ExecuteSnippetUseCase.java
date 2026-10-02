package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.ExecuteSnippetCommand;
import com.antigravity.bytebuddy.mcp.application.dto.SnippetExecutionResponse;

/**
 * Inbound port for compiling and running raw Java code snippets using official ByteBuddy APIs directly.
 */
public interface ExecuteSnippetUseCase {

    SnippetExecutionResponse executeSnippet(ExecuteSnippetCommand command);
}
