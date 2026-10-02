package com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;
import com.antigravity.bytebuddy.mcp.domain.model.InterceptionRule;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution.IsolatedDynamicExecutionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ByteBuddyEngineAdapterEdgeCasesTest {

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
    @DisplayName("SUPPRESS_EXCEPTIONS advice should catch and swallow thrown runtime exceptions")
    void testExceptionSuppressionAdvice() {
        ClassName cn = ClassName.of("com.demo.FailingService");
        MethodDefinition method = MethodDefinition.throwing("failFast", "String", "Critical failure!");

        BytecodeClass initial = engine.generateClass(cn, "java.lang.Object", List.of(), List.of(), List.of(method));

        // Initial method execution throws exception
        ExecutionResult rawResult = executionAdapter.execute(initial, "failFast", new Object[0]);
        assertThat(rawResult.isSuccess()).isFalse();
        assertThat(rawResult.getErrorMessage()).contains("Critical failure!");

        // Weave exception suppression advice
        InterceptionRule suppressRule = InterceptionRule.suppressExceptions("failFast");
        BytecodeClass transformed = engine.transformBytecode(initial, List.of(suppressRule));

        // Execution of transformed class suppresses exception and succeeds!
        ExecutionResult suppressedResult = executionAdapter.execute(transformed, "failFast", new Object[0]);
        assertThat(suppressedResult.isSuccess()).isTrue();
        assertThat(suppressedResult.getReturnValue()).isNull();
    }

    @Test
    @DisplayName("Transforming with regex pattern that matches no methods should leave bytecode intact without error")
    void testTransformWithNonMatchingRegex() {
        ClassName cn = ClassName.of("com.demo.NonMatchingTest");
        MethodDefinition method = MethodDefinition.returningConstant("doWork", "String", "working");

        BytecodeClass initial = engine.generateClass(cn, "java.lang.Object", List.of(), List.of(), List.of(method));

        InterceptionRule rule = InterceptionRule.measureTime("nonExistentMethodRegex.*");
        BytecodeClass transformed = engine.transformBytecode(initial, List.of(rule));

        assertThat(transformed).isNotNull();
        ExecutionResult result = executionAdapter.execute(transformed, "doWork", new Object[0]);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isEqualTo("working");
    }

    @Test
    @DisplayName("Multiple interception rules should be weaved sequentially onto matching methods")
    void testMultipleInterceptionRules() {
        ClassName cn = ClassName.of("com.demo.MultiAdviceService");
        MethodDefinition method = MethodDefinition.returningConstant("executeAction", "String", "done");

        BytecodeClass initial = engine.generateClass(cn, "java.lang.Object", List.of(), List.of(), List.of(method));

        List<InterceptionRule> rules = List.of(
                InterceptionRule.logCall("executeAction"),
                InterceptionRule.measureTime("executeAction")
        );

        BytecodeClass transformed = engine.transformBytecode(initial, rules);
        assertThat(transformed.getBytecodeSize()).isGreaterThan(0);

        ExecutionResult result = executionAdapter.execute(transformed, "executeAction", new Object[0]);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isEqualTo("done");
    }

    @Test
    @DisplayName("OVERRIDE_RETURN_VALUE with null should return null properly")
    void testOverrideReturnValueNull() {
        ClassName cn = ClassName.of("com.demo.OverrideNullDemo");
        MethodDefinition method = MethodDefinition.returningConstant("getSecret", "String", "secret123");

        BytecodeClass initial = engine.generateClass(cn, "java.lang.Object", List.of(), List.of(), List.of(method));

        InterceptionRule overrideRule = InterceptionRule.overrideReturn("getSecret", null);
        BytecodeClass transformed = engine.transformBytecode(initial, List.of(overrideRule));

        ExecutionResult result = executionAdapter.execute(transformed, "getSecret", new Object[0]);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isNull();
    }

    @Test
    @DisplayName("Dynamic class should be able to implement interfaces like java.lang.Runnable")
    void testClassImplementingInterface() {
        ClassName cn = ClassName.of("com.demo.MyRunnableTask");
        MethodDefinition runMethod = MethodDefinition.returningDefault("run", "void", List.of());

        BytecodeClass generated = engine.generateClass(
                cn,
                "java.lang.Object",
                List.of("java.lang.Runnable"),
                List.of(),
                List.of(runMethod)
        );

        assertThat(generated.getStructure().getInterfaces()).contains("java.lang.Runnable");

        ExecutionResult result = executionAdapter.execute(generated, "run", new Object[0]);
        assertThat(result.isSuccess()).isTrue();
    }
}
