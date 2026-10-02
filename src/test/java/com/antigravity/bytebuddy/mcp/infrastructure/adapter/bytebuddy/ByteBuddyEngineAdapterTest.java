package com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;
import com.antigravity.bytebuddy.mcp.domain.model.FieldDefinition;
import com.antigravity.bytebuddy.mcp.domain.model.InterceptionRule;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution.IsolatedDynamicExecutionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ByteBuddyEngineAdapterTest {

    private AsmAndCfrDisassemblerAdapter disassembler;
    private ByteBuddyEngineAdapter engine;
    private IsolatedDynamicExecutionAdapter executionAdapter;

    @BeforeEach
    void setUp() {
        disassembler = new AsmAndCfrDisassemblerAdapter();
        engine = new ByteBuddyEngineAdapter(disassembler);
        executionAdapter = new IsolatedDynamicExecutionAdapter();
    }

    @Test
    @DisplayName("Should dynamically generate a class with constant-returning method, disassemble, and execute it")
    void testGenerateAndExecuteClass() {
        ClassName className = ClassName.of("com.antigravity.dynamic.TestGreeter");
        MethodDefinition method = MethodDefinition.returningConstant("greet", "String", "Hello from ByteBuddy MCP!");
        FieldDefinition field = FieldDefinition.of("callCount", "int");

        BytecodeClass generated = engine.generateClass(
                className,
                "java.lang.Object",
                List.of(),
                List.of(field),
                List.of(method)
        );

        assertThat(generated).isNotNull();
        assertThat(generated.getBytecodeSize()).isGreaterThan(0);
        assertThat(generated.getName()).isEqualTo(className);

        // Disassemble with ASM
        String opcodes = disassembler.disassembleToOpcodes(generated.getBytecode());
        assertThat(opcodes).contains("greet()Ljava/lang/String;");

        // Decompile with CFR
        String javaSource = disassembler.decompileToJava(generated.getBytecode(), className.getValue());
        assertThat(javaSource).contains("public class TestGreeter");

        // Execute dynamic method
        ExecutionResult result = executionAdapter.execute(generated, "greet", new Object[0]);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isEqualTo("Hello from ByteBuddy MCP!");
    }

    @Test
    @DisplayName("Should transform existing class bytecode by weaving timing advice")
    void testTransformBytecodeWithTiming() {
        ClassName className = ClassName.of("com.antigravity.dynamic.CalcService");
        MethodDefinition method = MethodDefinition.returningConstant("compute", "String", "42");

        BytecodeClass original = engine.generateClass(
                className,
                "java.lang.Object",
                List.of(),
                List.of(),
                List.of(method)
        );

        InterceptionRule rule = InterceptionRule.measureTime("compute");
        BytecodeClass transformed = engine.transformBytecode(original, List.of(rule));

        assertThat(transformed.getBytecodeSize()).isGreaterThan(0);

        ExecutionResult result = executionAdapter.execute(transformed, "compute", new Object[0]);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isEqualTo("42");
    }
}
