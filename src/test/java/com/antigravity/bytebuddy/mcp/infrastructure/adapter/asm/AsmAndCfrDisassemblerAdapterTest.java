package com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm;

import com.antigravity.bytebuddy.mcp.domain.exception.BytecodeTransformationException;
import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsmAndCfrDisassemblerAdapterTest {

    private AsmAndCfrDisassemblerAdapter disassembler;

    @BeforeEach
    void setUp() {
        disassembler = new AsmAndCfrDisassemblerAdapter();
    }

    @Test
    @DisplayName("disassembleToOpcodes should handle null or empty bytecode gracefully")
    void testDisassembleEmptyBytecode() {
        assertThat(disassembler.disassembleToOpcodes(null)).isEqualTo("// Empty bytecode");
        assertThat(disassembler.disassembleToOpcodes(new byte[0])).isEqualTo("// Empty bytecode");
    }

    @Test
    @DisplayName("disassembleToOpcodes should return error comment on corrupted bytecode instead of throwing")
    void testDisassembleCorruptBytecode() {
        byte[] corrupt = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        String result = disassembler.disassembleToOpcodes(corrupt);
        assertThat(result).startsWith("// Disassembly error:");
    }

    @Test
    @DisplayName("decompileToJava should handle null or empty bytecode gracefully")
    void testDecompileEmptyBytecode() {
        assertThat(disassembler.decompileToJava(null, "Test")).isEqualTo("// No bytecode provided");
        assertThat(disassembler.decompileToJava(new byte[0], "Test")).isEqualTo("// No bytecode provided");
    }

    @Test
    @DisplayName("decompileToJava should return notice on invalid bytecode without throwing")
    void testDecompileCorruptBytecode() {
        byte[] corrupt = new byte[]{0x00, 0x01, 0x02, 0x03};
        String result = disassembler.decompileToJava(corrupt, "CorruptClass");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("extractMajorVersion should return 0 for short or null bytes and correct version for valid headers")
    void testExtractMajorVersionEdgeCases() {
        assertThat(disassembler.extractMajorVersion(null)).isEqualTo(0);
        assertThat(disassembler.extractMajorVersion(new byte[]{1, 2, 3})).isEqualTo(0);

        byte[] java17Header = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 61};
        assertThat(disassembler.extractMajorVersion(java17Header)).isEqualTo(61);
    }

    @Test
    @DisplayName("parseStructure should throw BytecodeTransformationException on null, short, or invalid bytecode")
    void testParseStructureInvalidBytecode() {
        assertThatThrownBy(() -> disassembler.parseStructure(null))
                .isInstanceOf(BytecodeTransformationException.class);

        assertThatThrownBy(() -> disassembler.parseStructure(new byte[]{1, 2}))
                .isInstanceOf(BytecodeTransformationException.class);

        byte[] garbage = new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, 1, 2, 3, 4};
        assertThatThrownBy(() -> disassembler.parseStructure(garbage))
                .isInstanceOf(BytecodeTransformationException.class)
                .hasMessageContaining("Failed to parse bytecode structure");
    }
}
