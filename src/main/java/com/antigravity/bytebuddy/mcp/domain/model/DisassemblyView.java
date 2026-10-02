package com.antigravity.bytebuddy.mcp.domain.model;

import java.io.Serializable;

/**
 * Value Object presenting bytecode disassembly and decompiled Java representations.
 */
public final class DisassemblyView implements Serializable {
    private final ClassName className;
    private final String decompiledJava;
    private final String asmOpcodes;
    private final int bytecodeSize;

    public DisassemblyView(ClassName className, String decompiledJava, String asmOpcodes, int bytecodeSize) {
        this.className = className;
        this.decompiledJava = decompiledJava == null ? "" : decompiledJava;
        this.asmOpcodes = asmOpcodes == null ? "" : asmOpcodes;
        this.bytecodeSize = bytecodeSize;
    }

    public ClassName getClassName() {
        return className;
    }

    public String getDecompiledJava() {
        return decompiledJava;
    }

    public String getAsmOpcodes() {
        return asmOpcodes;
    }

    public int getBytecodeSize() {
        return bytecodeSize;
    }
}
