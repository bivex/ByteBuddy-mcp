package com.antigravity.bytebuddy.mcp.adapter.mcp.tools;

import com.antigravity.bytebuddy.mcp.application.service.ClassSessionService;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm.AsmAndCfrDisassemblerAdapter;
import com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence.InMemoryClassRepositoryAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

class LoadFromJarTest {

    @Test
    @DisplayName("Should list classes and load specific class from a real JAR archive")
    void testLoadFromJar() throws Exception {
        // Create a temporary valid test JAR with a dummy class
        Path tempJar = Files.createTempFile("test_", ".jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        ClassRepositoryPort repo = new InMemoryClassRepositoryAdapter();
        BytecodeDisassemblerPort disasm = new AsmAndCfrDisassemblerAdapter();
        com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy.ByteBuddyEngineAdapter engine =
                new com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy.ByteBuddyEngineAdapter(disasm);

        byte[] dummyBytecode = engine.generateClass(
                com.antigravity.bytebuddy.mcp.domain.model.ClassName.of("com.example.TestInJar"),
                "java.lang.Object",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
        ).getBytecode();

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tempJar), manifest)) {
            JarEntry entry = new JarEntry("com/example/TestInJar.class");
            jos.putNextEntry(entry);
            jos.write(dummyBytecode);
            jos.closeEntry();
        }

        ClassSessionService sessionService = new ClassSessionService(repo, disasm);
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LoadFromJarTool tool = new LoadFromJarTool(sessionService, mapper);

        // 1. List entries
        String listOutput = tool.execute(mapper.readTree("{\"jarPath\": \"" + tempJar.toAbsolutePath() + "\"}"));
        assertThat(listOutput).contains("com.example.TestInJar");

        // 2. Load entry
        String loadOutput = tool.execute(mapper.readTree("{\"jarPath\": \"" + tempJar.toAbsolutePath() + "\", \"className\": \"com.example.TestInJar\"}"));
        assertThat(loadOutput).contains("com.example.TestInJar");

        Files.deleteIfExists(tempJar);
    }
}
