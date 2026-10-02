package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.ExecuteSnippetCommand;
import com.antigravity.bytebuddy.mcp.application.dto.SnippetExecutionResponse;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.SnippetCompilerPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.compiler.JdkInMemoryCompilerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution.IsolatedDynamicExecutionAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecuteSnippetServiceTest {

    private ExecuteSnippetService snippetService;
    private ClassRepositoryPort repositoryPort;
    private IsolatedDynamicExecutionAdapter executionAdapter;

    @BeforeEach
    void setUp() {
        repositoryPort = new InMemoryClassRepositoryAdapter();
        BytecodeDisassemblerPort disassemblerPort = new AsmAndCfrDisassemblerAdapter();
        SnippetCompilerPort compilerPort = new JdkInMemoryCompilerAdapter();

        snippetService = new ExecuteSnippetService(compilerPort, disassemblerPort, repositoryPort);
        executionAdapter = new IsolatedDynamicExecutionAdapter(repositoryPort);
    }

    @Test
    @DisplayName("Should compile and execute raw ByteBuddy fluent API code directly, then register and invoke it")
    void testExecuteRawByteBuddySnippet() {
        String snippet = """
                DynamicType.Unloaded<?> unloaded = new ByteBuddy(ClassFileVersion.JAVA_V17)
                    .subclass(Object.class)
                    .name("com.demo.FluentGreeter")
                    .method(named("toString"))
                    .intercept(FixedValue.value("Dynamic greeting via raw ByteBuddy API!"))
                    .make();
                return unloaded;
                """;

        ExecuteSnippetCommand command = new ExecuteSnippetCommand(snippet, null);
        SnippetExecutionResponse response = snippetService.executeSnippet(command);

        assertThat(response.success()).isTrue();
        assertThat(response.registeredClasses()).contains("com.demo.FluentGreeter");

        // Verify that the class was saved to the session repository
        BytecodeClass stored = repositoryPort.findByName(ClassName.of("com.demo.FluentGreeter")).orElse(null);
        assertThat(stored).isNotNull();
        assertThat(stored.getBytecodeSize()).isGreaterThan(0);

        // Execute the toString method on the generated class
        ExecutionResult execResult = executionAdapter.execute(stored, "toString", new Object[0]);
        assertThat(execResult.isSuccess()).isTrue();
        assertThat(execResult.getReturnValue()).isEqualTo("Dynamic greeting via raw ByteBuddy API!");
    }
}
