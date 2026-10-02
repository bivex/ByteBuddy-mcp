package com.antigravity.bytebuddy.mcp.domain.model;

import com.antigravity.bytebuddy.mcp.domain.exception.BytecodeTransformationException;
import com.antigravity.bytebuddy.mcp.domain.exception.InvalidClassNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelTest {

    @Test
    @DisplayName("ClassName should properly parse valid identifiers and extract package/simple names")
    void testValidClassName() {
        ClassName cn = ClassName.of("com.example.service.UserService");
        assertThat(cn.getValue()).isEqualTo("com.example.service.UserService");
        assertThat(cn.getSimpleName()).isEqualTo("UserService");
        assertThat(cn.getPackageName()).isEqualTo("com.example.service");
        assertThat(cn.getInternalName()).isEqualTo("com/example/service/UserService");
    }

    @Test
    @DisplayName("ClassName should reject blank or illegal identifiers")
    void testInvalidClassName() {
        assertThatThrownBy(() -> ClassName.of(""))
                .isInstanceOf(InvalidClassNameException.class);
        assertThatThrownBy(() -> ClassName.of("123Illegal.Class"))
                .isInstanceOf(InvalidClassNameException.class);
        assertThatThrownBy(() -> ClassName.of("com.example..Service"))
                .isInstanceOf(InvalidClassNameException.class);
    }

    @Test
    @DisplayName("BytecodeClass should validate CAFEBABE magic header and maintain aggregate invariants")
    void testBytecodeClassHeaderValidation() {
        byte[] validBytecode = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 61};
        ClassName cn = ClassName.of("com.example.TestClass");
        BytecodeClass bc = BytecodeClass.of(cn, validBytecode, 61, ClassStructure.empty("java.lang.Object"));

        assertThat(bc.getName()).isEqualTo(cn);
        assertThat(bc.getBytecodeSize()).isEqualTo(8);
        assertThat(bc.getMajorVersion()).isEqualTo(61);

        byte[] invalidHeader = new byte[]{0x00, 0x01, 0x02, 0x03, 0, 0, 0, 61};
        assertThatThrownBy(() -> BytecodeClass.of(cn, invalidHeader, 61, null))
                .isInstanceOf(BytecodeTransformationException.class)
                .hasMessageContaining("0x00010203");
    }
}
