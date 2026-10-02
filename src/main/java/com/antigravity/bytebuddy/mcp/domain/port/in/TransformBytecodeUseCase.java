package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.TransformBytecodeCommand;
import com.antigravity.bytebuddy.mcp.application.dto.TransformBytecodeResponse;

/**
 * Inbound port for transforming existing bytecode using ByteBuddy.
 */
public interface TransformBytecodeUseCase {

    TransformBytecodeResponse transform(TransformBytecodeCommand command);
}
