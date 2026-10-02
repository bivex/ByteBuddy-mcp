package com.antigravity.bytebuddy.mcp.domain.port.out;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;

/**
 * Secondary (outbound) port defining dynamic execution of classes in an isolated runtime environment.
 */
public interface DynamicExecutionPort {

    ExecutionResult execute(BytecodeClass bytecodeClass, String methodName, Object[] args);
}
