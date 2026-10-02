package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.InspectCommand;
import com.antigravity.bytebuddy.mcp.application.dto.InspectionResponse;

/**
 * Inbound port for inspecting class bytecode structure, methods, fields, and hierarchy.
 */
public interface InspectBytecodeUseCase {

    InspectionResponse inspect(InspectCommand command);
}
