package com.antigravity.bytebuddy.mcp.domain.port.in;

import com.antigravity.bytebuddy.mcp.application.dto.DisassembleCommand;
import com.antigravity.bytebuddy.mcp.application.dto.DisassemblyResponse;

/**
 * Inbound port for disassembling and decompiling class bytecode.
 */
public interface DisassembleBytecodeUseCase {

    DisassemblyResponse disassemble(DisassembleCommand command);
}
