package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.ExecuteMethodCommand;
import com.antigravity.bytebuddy.mcp.application.dto.ExecutionResponse;

/**
 * Inbound port for dynamically invoking methods of generated or transformed classes.
 */
public interface ExecuteDynamicMethodUseCase {

    ExecutionResponse executeMethod(ExecuteMethodCommand command);
}
