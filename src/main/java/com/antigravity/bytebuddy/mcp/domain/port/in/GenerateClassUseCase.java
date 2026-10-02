package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.GenerateClassCommand;
import com.antigravity.bytebuddy.mcp.application.dto.GeneratedClassResponse;

/**
 * Inbound port for dynamic class generation using ByteBuddy.
 */
public interface GenerateClassUseCase {

    GeneratedClassResponse generateClass(GenerateClassCommand command);
}
