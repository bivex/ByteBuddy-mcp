package com.antigravity.bytebuddy.mcp.domain.model;

import com.antigravity.bytebuddy.mcp.domain.exception.BytecodeTransformationException;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Aggregate Root representing a compiled or dynamically synthesized Java class in bytecode format.
 */
public class BytecodeClass implements Serializable {
    private static final int JAVA_MAGIC = 0xCAFEBABE;

    private final ClassName name;
    private byte[] bytecode;
    private final int majorVersion;
    private ClassStructure structure;
    private final Instant createdAt;
    private Instant updatedAt;

    public BytecodeClass(ClassName name, byte[] bytecode, int majorVersion, ClassStructure structure) {
        this.name = Objects.requireNonNull(name, "ClassName cannot be null");
        this.bytecode = validateAndCloneBytecode(bytecode);
        this.majorVersion = majorVersion;
        this.structure = structure != null ? structure : ClassStructure.empty("java.lang.Object");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static BytecodeClass of(ClassName name, byte[] bytecode, int majorVersion, ClassStructure structure) {
        return new BytecodeClass(name, bytecode, majorVersion, structure);
    }

    private static byte[] validateAndCloneBytecode(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            throw new BytecodeTransformationException("Bytecode is empty or too short to be a valid classfile");
        }
        int magic = ((bytes[0] & 0xFF) << 24) |
                    ((bytes[1] & 0xFF) << 16) |
                    ((bytes[2] & 0xFF) << 8)  |
                    (bytes[3] & 0xFF);
        if (magic != JAVA_MAGIC) {
            throw new BytecodeTransformationException(
                    String.format("Invalid class file magic header: 0x%08X (expected 0xCAFEBABE)", magic));
        }
        return bytes.clone();
    }

    /**
     * Updates the bytecode of the aggregate, e.g. after a transformation or instrumentation pass.
     */
    public void updateBytecode(byte[] newBytecode, ClassStructure newStructure) {
        this.bytecode = validateAndCloneBytecode(newBytecode);
        if (newStructure != null) {
            this.structure = newStructure;
        }
        this.updatedAt = Instant.now();
    }

    public ClassName getName() {
        return name;
    }

    public byte[] getBytecode() {
        return bytecode.clone();
    }

    public int getBytecodeSize() {
        return bytecode.length;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public ClassStructure getStructure() {
        return structure;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BytecodeClass that = (BytecodeClass) o;
        return Objects.equals(name, that.name) && Arrays.equals(bytecode, that.bytecode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "BytecodeClass{" +
                "name=" + name +
                ", size=" + bytecode.length + " bytes" +
                ", majorVersion=" + majorVersion +
                '}';
    }
}
