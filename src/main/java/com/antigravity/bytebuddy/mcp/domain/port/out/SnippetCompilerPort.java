package com.antigravity.bytebuddy.mcp.domain.port.out;

import java.util.Map;

/**
 * Secondary port for compiling Java source code in memory.
 */
public interface SnippetCompilerPort {

    /**
     * Compiles Java source code in memory and returns class name to bytecode mapping.
     */
    Map<String, byte[]> compile(String className, String sourceCode);
}
