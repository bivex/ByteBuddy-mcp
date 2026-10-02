package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.ClassSummaryResponse;
import com.antigravity.bytebuddy.mcp.application.dto.LoadBytesCommand;
import com.antigravity.bytebuddy.mcp.domain.exception.ClassNotFoundDomainException;
import com.antigravity.bytebuddy.mcp.domain.exception.DomainException;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassSessionServiceEdgeCasesTest {

    private ClassRepositoryPort repositoryPort;
    private BytecodeDisassemblerPort disassemblerPort;
    private ClassSessionService sessionService;

    @BeforeEach
    void setUp() {
        repositoryPort = new InMemoryClassRepositoryAdapter();
        disassemblerPort = new AsmAndCfrDisassemblerAdapter();
        sessionService = new ClassSessionService(repositoryPort, disassemblerPort);
    }

    @Test
    @DisplayName("loadFromJar should throw DomainException when JAR file does not exist")
    void testLoadFromNonExistentJar() {
        assertThatThrownBy(() -> sessionService.loadFromJar("/non/existent/path/to/library.jar", "SomeClass"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Failed to read JAR");
    }

    @Test
    @DisplayName("loadFromJar should throw DomainException when target file is not a valid zip/jar")
    void testLoadFromCorruptedOrTextFile() throws IOException {
        Path tempTextFile = Files.createTempFile("fake_jar_", ".jar");
        Files.writeString(tempTextFile, "This is not a zip or jar archive!");

        assertThatThrownBy(() -> sessionService.loadFromJar(tempTextFile.toAbsolutePath().toString(), "SomeClass"))
                .isInstanceOf(DomainException.class);

        Files.deleteIfExists(tempTextFile);
    }

    @Test
    @DisplayName("loadFromJar should throw ClassNotFoundDomainException when specified class is not in JAR")
    void testLoadClassNotInJar() throws IOException {
        Path emptyJar = Files.createTempFile("empty_", ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(emptyJar), manifest)) {
            // Empty JAR with only manifest
        }

        assertThatThrownBy(() -> sessionService.loadFromJar(emptyJar.toAbsolutePath().toString(), "com.missing.Class"))
                .isInstanceOf(ClassNotFoundDomainException.class)
                .hasMessageContaining("not found in JAR");

        Files.deleteIfExists(emptyJar);
    }

    @Test
    @DisplayName("loadClassBytes should throw IllegalArgumentException when base64 is malformed")
    void testLoadMalformedBase64() {
        LoadBytesCommand command = new LoadBytesCommand("BadBase64", "!!!not-valid-base-64!!!");
        assertThatThrownBy(() -> sessionService.loadClassBytes(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("loadClassBytes should throw BytecodeTransformationException when bytes are not a valid class file")
    void testLoadInvalidClassBytes() {
        String base64NotAClass = Base64.getEncoder().encodeToString("just plain text string".getBytes());
        LoadBytesCommand command = new LoadBytesCommand("com.demo.NotAClass", base64NotAClass);

        assertThatThrownBy(() -> sessionService.loadClassBytes(command))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("removeClass should return false when class does not exist in repository")
    void testRemoveNonExistentClass() {
        boolean removed = sessionService.removeClass("com.nonexistent.Class");
        assertThat(removed).isFalse();
    }

    @Test
    @DisplayName("clearSession should remove all classes from the repository")
    void testClearSession() {
        // Save dummy valid class header
        byte[] validBytecode = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 61};
        // Generate valid class
        var engine = new com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy.ByteBuddyEngineAdapter(disassemblerPort);
        var bc = engine.generateClass(
                com.antigravity.bytebuddy.mcp.domain.model.ClassName.of("com.demo.SessionItem"),
                "java.lang.Object",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
        );
        repositoryPort.save(bc);
        assertThat(sessionService.listClasses()).hasSize(1);

        sessionService.clearSession();
        assertThat(sessionService.listClasses()).isEmpty();
    }
}
