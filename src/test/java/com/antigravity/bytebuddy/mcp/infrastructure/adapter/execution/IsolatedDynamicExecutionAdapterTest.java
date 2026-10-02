package com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy.ByteBuddyEngineAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.compiler.JdkInMemoryCompilerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IsolatedDynamicExecutionAdapterTest {

    private ClassRepositoryPort repositoryPort;
    private ByteBuddyEngineAdapter engine;
    private AsmAndCfrDisassemblerAdapter disassembler;
    private JdkInMemoryCompilerAdapter compiler;
    private IsolatedDynamicExecutionAdapter executionAdapter;

    @BeforeEach
    void setUp() {
        repositoryPort = new InMemoryClassRepositoryAdapter();
        disassembler = new AsmAndCfrDisassemblerAdapter();
        engine = new ByteBuddyEngineAdapter(disassembler);
        compiler = new JdkInMemoryCompilerAdapter();
        executionAdapter = new IsolatedDynamicExecutionAdapter(repositoryPort);
    }

    @Test
    @DisplayName("Should return failure result when method does not exist on class")
    void testMethodNotFound() {
        ClassName cn = ClassName.of("com.demo.Simple");
        BytecodeClass bc = engine.generateClass(cn, "java.lang.Object", List.of(), List.of(), List.of());

        ExecutionResult result = executionAdapter.execute(bc, "nonExistentMethod", new Object[0]);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Method 'nonExistentMethod' not found");
        assertThat(result.getExceptionClassName()).isEqualTo(NoSuchMethodException.class.getName());
    }

    @Test
    @DisplayName("Should capture exception details when invoked method throws a RuntimeException")
    void testMethodThrowingException() {
        String source = """
                package com.demo;
                public class Thrower {
                    public static void explode() {
                        throw new ArithmeticException("Division by zero in dynamic execution");
                    }
                }
                """;
        Map<String, byte[]> compiled = compiler.compile("com.demo.Thrower", source);
        byte[] bytes = compiled.get("com.demo.Thrower");
        BytecodeClass bc = BytecodeClass.of(ClassName.of("com.demo.Thrower"), bytes, 61, disassembler.parseStructure(bytes));

        ExecutionResult result = executionAdapter.execute(bc, "explode", new Object[0]);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Division by zero in dynamic execution");
        assertThat(result.getExceptionClassName()).isEqualTo(ArithmeticException.class.getName());
    }

    @Test
    @DisplayName("Should coerce argument types (e.g. Integer to long/double) for primitive parameters")
    void testArgumentCoercion() {
        String source = """
                package com.demo;
                public class Calculator {
                    public static String calculate(long id, double factor, boolean active, String label) {
                        return "id=" + id + ",factor=" + factor + ",active=" + active + ",label=" + label;
                    }
                }
                """;
        Map<String, byte[]> compiled = compiler.compile("com.demo.Calculator", source);
        byte[] bytes = compiled.get("com.demo.Calculator");
        BytecodeClass bc = BytecodeClass.of(ClassName.of("com.demo.Calculator"), bytes, 61, disassembler.parseStructure(bytes));

        // Passing Integer(42), Integer(2), Boolean.TRUE, "Test"
        Object[] args = new Object[]{42, 2, true, "Test"};
        ExecutionResult result = executionAdapter.execute(bc, "calculate", args);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isEqualTo("id=42,factor=2.0,active=true,label=Test");
    }

    @Test
    @DisplayName("Should correctly distinguish and invoke overloaded methods matching parameter types")
    void testOverloadedMethodsResolution() {
        String source = """
                package com.demo;
                public class OverloadDemo {
                    public static String process(String str) {
                        return "STRING:" + str;
                    }
                    public static String process(int num) {
                        return "INT:" + num;
                    }
                }
                """;
        Map<String, byte[]> compiled = compiler.compile("com.demo.OverloadDemo", source);
        byte[] bytes = compiled.get("com.demo.OverloadDemo");
        BytecodeClass bc = BytecodeClass.of(ClassName.of("com.demo.OverloadDemo"), bytes, 61, disassembler.parseStructure(bytes));

        ExecutionResult resString = executionAdapter.execute(bc, "process", new Object[]{"hello"});
        assertThat(resString.isSuccess()).isTrue();
        assertThat(resString.getReturnValue()).isEqualTo("STRING:hello");

        ExecutionResult resInt = executionAdapter.execute(bc, "process", new Object[]{999});
        assertThat(resInt.isSuccess()).isTrue();
        assertThat(resInt.getReturnValue()).isEqualTo("INT:999");
    }

    @Test
    @DisplayName("Should gracefully handle instance method execution on class lacking a no-arg constructor")
    void testInstanceMethodWithoutNoArgConstructor() {
        String source = """
                package com.demo;
                public class CustomCtor {
                    private final String val;
                    public CustomCtor(String val) { this.val = val; }
                    public String getVal() { return this.val; }
                }
                """;
        Map<String, byte[]> compiled = compiler.compile("com.demo.CustomCtor", source);
        byte[] bytes = compiled.get("com.demo.CustomCtor");
        BytecodeClass bc = BytecodeClass.of(ClassName.of("com.demo.CustomCtor"), bytes, 61, disassembler.parseStructure(bytes));

        ExecutionResult result = executionAdapter.execute(bc, "getVal", new Object[0]);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("does not define a no-arg constructor");
    }

    @Test
    @DisplayName("Should truncate huge standard output without memory exhaustion")
    void testOutputTruncationOnMassivePrinting() {
        String source = """
                package com.demo;
                public class VerbosePrinter {
                    public static void printLots() {
                        for (int i = 0; i < 50000; i++) {
                            System.out.println("Line: " + i + " - Some repetitive long log message that is definitely over eighty characters long to cross two megabytes limit quickly!");
                        }
                    }
                }
                """;
        Map<String, byte[]> compiled = compiler.compile("com.demo.VerbosePrinter", source);
        byte[] bytes = compiled.get("com.demo.VerbosePrinter");
        BytecodeClass bc = BytecodeClass.of(ClassName.of("com.demo.VerbosePrinter"), bytes, 61, disassembler.parseStructure(bytes));

        ExecutionResult result = executionAdapter.execute(bc, "printLots", new Object[0]);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCapturedOutput()).contains("[Output truncated");
        assertThat(result.getCapturedOutput().length()).isLessThanOrEqualTo(2 * 1024 * 1024 + 1000);
    }

    @Test
    @DisplayName("Should allow dynamic classes to resolve other session classes as cross-class dependencies")
    void testCrossClassDependencies() {
        String source = """
                package com.demo;
                class Helper {
                    public static String help() { return "Help from Session Helper!"; }
                }
                public class Consumer {
                    public static String consume() {
                        return Helper.help();
                    }
                }
                """;
        Map<String, byte[]> compiled = compiler.compile("com.demo.Consumer", source);
        byte[] helperBytes = compiled.get("com.demo.Helper");
        BytecodeClass helperBc = BytecodeClass.of(ClassName.of("com.demo.Helper"), helperBytes, 61, disassembler.parseStructure(helperBytes));
        repositoryPort.save(helperBc);

        byte[] consumerBytes = compiled.get("com.demo.Consumer");
        BytecodeClass consumerBc = BytecodeClass.of(ClassName.of("com.demo.Consumer"), consumerBytes, 61, disassembler.parseStructure(consumerBytes));

        ExecutionResult result = executionAdapter.execute(consumerBc, "consume", new Object[0]);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReturnValue()).isEqualTo("Help from Session Helper!");
    }
}
