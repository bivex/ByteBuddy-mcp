package com.antigravity.bytebuddy.mcp.domain.port.out;

import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;

/**
 * Secondary (outbound) port defining disassembly (ASM) and decompilation (CFR) capabilities.
 */
public interface BytecodeDisassemblerPort {

    String disassembleToOpcodes(byte[] bytecode);

    String decompileToJava(byte[] bytecode, String className);

    ClassStructure parseStructure(byte[] bytecode);

    int extractMajorVersion(byte[] bytecode);
}
