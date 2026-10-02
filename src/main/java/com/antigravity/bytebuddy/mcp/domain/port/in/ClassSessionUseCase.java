package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.ClassSummaryResponse;
import com.antigravity.bytebuddy.mcp.application.dto.LoadBytesCommand;

import java.util.List;

/**
 * Inbound port for managing in-session bytecode classes.
 */
public interface ClassSessionUseCase {

    ClassSummaryResponse loadClassBytes(LoadBytesCommand command);

    List<ClassSummaryResponse> listClasses();

    boolean removeClass(String className);
}
