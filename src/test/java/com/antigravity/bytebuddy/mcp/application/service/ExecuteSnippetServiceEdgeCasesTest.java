package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.ExecuteSnippetCommand;
import com.antigravity.bytebuddy.mcp.application.dto.SnippetExecutionResponse;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.SnippetCompilerPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.compiler.JdkInMemoryCompilerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecuteSnippetServiceEdgeCasesTest {

    private ExecuteSnippetService snippetService;
    private ClassRepositoryPort repositoryPort;

    @BeforeEach
    void setUp() {
        repositoryPort = new InMemoryClassRepositoryAdapter();
        BytecodeDisassemblerPort disassemblerPort = new AsmAndCfrDisassemblerAdapter();
        SnippetCompilerPort compilerPort = new JdkInMemoryCompilerAdapter();

        snippetService = new ExecuteSnippetService(compilerPort, disassemblerPort, repositoryPort);
    }

    @Test
    @DisplayName("Should return failure response with compiler diagnostics when snippet has syntax error")
    void testSnippetCompilationError() {
        String invalidSnippet = """
                this is total nonsense java code !!! @#$%^
                """;

        SnippetExecutionResponse response = snippetService.executeSnippet(new ExecuteSnippetCommand(invalidSnippet, null));

        assertThat(response.success()).isFalse();
        assertThat(response.errorMessage()).isNotNull();
        assertThat(response.errorMessage()).contains("Compilation failed");
    }

    @Test
    @DisplayName("Should return failure response with exception details when snippet throws runtime exception")
    void testSnippetRuntimeException() {
        String throwingSnippet = """
                if (true) {
                    throw new UnsupportedOperationException("Intentional runtime explosion inside snippet");
                }
                return null;
                """;

        SnippetExecutionResponse response = snippetService.executeSnippet(new ExecuteSnippetCommand(throwingSnippet, null));

        assertThat(response.success()).isFalse();
        assertThat(response.errorMessage()).contains("Intentional runtime explosion inside snippet");
    }

    @Test
    @DisplayName("Should execute a full custom class declaring a public static void main entrypoint")
    void testFullClassWithMainMethod() {
        String fullClass = """
                package com.demo;
                public class CustomMainApp {
                    public static void main(String[] args) {
                        System.out.println("Hello from main method inside snippet!");
                    }
                }
                """;

        SnippetExecutionResponse response = snippetService.executeSnippet(new ExecuteSnippetCommand(fullClass, "com.demo.CustomMainApp"));

        assertThat(response.success()).isTrue();
        assertThat(response.capturedOutput()).contains("Hello from main method inside snippet!");
        assertThat(response.registeredClasses()).contains("com.demo.CustomMainApp");
    }

    @Test
    @DisplayName("Should execute a full custom class declaring a public Object run entrypoint")
    void testFullClassWithRunMethod() {
        String fullClass = """
                package com.demo;
                public class CustomRunnerApp {
                    public String run() {
                        return "Result from run method!";
                    }
                }
                """;

        SnippetExecutionResponse response = snippetService.executeSnippet(new ExecuteSnippetCommand(fullClass, "com.demo.CustomRunnerApp"));

        assertThat(response.success()).isTrue();
        assertThat(response.returnValue()).isEqualTo("Result from run method!");
        assertThat(response.registeredClasses()).contains("com.demo.CustomRunnerApp");
    }

    @Test
    @DisplayName("Should truncate output safely when snippet outputs excessive data")
    void testSnippetExcessiveOutputTruncation() {
        String snippet = """
                for (int i = 0; i < 50000; i++) {
                    System.out.println("Excessive line " + i + " with very long padding text designed to exceed two megabytes buffer safety limit easily!");
                }
                return "Finished";
                """;

        SnippetExecutionResponse response = snippetService.executeSnippet(new ExecuteSnippetCommand(snippet, null));

        assertThat(response.success()).isTrue();
        assertThat(response.capturedOutput()).contains("[Output truncated");
        assertThat(response.returnValue()).isEqualTo("Finished");
    }
}
