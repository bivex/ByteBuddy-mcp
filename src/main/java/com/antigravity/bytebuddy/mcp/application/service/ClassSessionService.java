package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.ClassSummaryResponse;
import com.antigravity.bytebuddy.mcp.application.dto.LoadBytesCommand;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import com.antigravity.bytebuddy.mcp.domain.port.in.ClassSessionUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ClassSessionService implements ClassSessionUseCase {
    private static final Logger log = LoggerFactory.getLogger(ClassSessionService.class);

    private final ClassRepositoryPort repositoryPort;
    private final BytecodeDisassemblerPort disassemblerPort;

    public ClassSessionService(ClassRepositoryPort repositoryPort,
                              BytecodeDisassemblerPort disassemblerPort) {
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
    }

    @Override
    public ClassSummaryResponse loadClassBytes(LoadBytesCommand command) {
        byte[] bytes = Base64.getDecoder().decode(command.bytecodeBase64().trim());
        ClassStructure structure = disassemblerPort.parseStructure(bytes);
        int version = disassemblerPort.extractMajorVersion(bytes);

        ClassName name;
        if (command.className() != null && !command.className().isBlank()) {
            name = ClassName.of(command.className());
        } else {
            name = ClassName.of("LoadedClass_" + System.currentTimeMillis());
        }

        BytecodeClass bytecodeClass = BytecodeClass.of(name, bytes, version, structure);
        repositoryPort.save(bytecodeClass);
        log.info("Saved loaded class '{}' ({} bytes) to session", name, bytes.length);

        return toSummary(bytecodeClass);
    }

    @Override
    public ClassSummaryResponse loadFromJar(String jarPath, String className) {
        if ("*".equals(className) || "all".equalsIgnoreCase(className)) {
            int count = 0;
            try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarPath))) {
                for (var it = jarFile.entries().asIterator(); it.hasNext(); ) {
                    var entry = it.next();
                    if (entry.getName().endsWith(".class") &&
                            !entry.getName().startsWith("META-INF/") &&
                            !entry.getName().endsWith("package-info.class") &&
                            !entry.getName().endsWith("module-info.class")) {
                        String fqcn = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                        byte[] bytes;
                        try (var is = jarFile.getInputStream(entry)) {
                            bytes = is.readAllBytes();
                        }
                        ClassStructure structure = disassemblerPort.parseStructure(bytes);
                        int version = disassemblerPort.extractMajorVersion(bytes);
                        BytecodeClass bc = BytecodeClass.of(ClassName.of(fqcn), bytes, version, structure);
                        repositoryPort.save(bc);
                        count++;
                    }
                }
                log.info("Batch loaded {} classes from JAR '{}'", count, jarPath);
                return new ClassSummaryResponse(
                        "ALL_CLASSES_FROM:" + new java.io.File(jarPath).getName(),
                        count,
                        52,
                        count,
                        0,
                        java.time.Instant.now()
                );
            } catch (java.io.IOException e) {
                throw new com.antigravity.bytebuddy.mcp.domain.exception.DomainException("Failed to read JAR: " + e.getMessage(), e) {};
            }
        }

        String internalPath = className.replace('.', '/') + (className.endsWith(".class") ? "" : ".class");
        String normalizedName = className.endsWith(".class") ? className.substring(0, className.length() - 6).replace('/', '.') : className.replace('/', '.');

        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarPath))) {
            var entry = jarFile.getJarEntry(internalPath);
            if (entry == null) {
                // Try searching case-insensitively or matching suffix
                for (var it = jarFile.entries().asIterator(); it.hasNext(); ) {
                    var e = it.next();
                    if (e.getName().equalsIgnoreCase(internalPath) || e.getName().endsWith("/" + internalPath)) {
                        entry = e;
                        break;
                    }
                }
            }
            if (entry == null) {
                throw new com.antigravity.bytebuddy.mcp.domain.exception.ClassNotFoundDomainException(
                        "Class entry '" + internalPath + "' not found in JAR: " + jarPath);
            }

            byte[] bytes;
            try (var is = jarFile.getInputStream(entry)) {
                bytes = is.readAllBytes();
            }

            ClassStructure structure = disassemblerPort.parseStructure(bytes);
            int version = disassemblerPort.extractMajorVersion(bytes);
            ClassName name = ClassName.of(normalizedName);

            BytecodeClass bytecodeClass = BytecodeClass.of(name, bytes, version, structure);
            repositoryPort.save(bytecodeClass);
            log.info("Loaded class '{}' from JAR '{}'", name, jarPath);

            return toSummary(bytecodeClass);
        } catch (java.io.IOException e) {
            throw new com.antigravity.bytebuddy.mcp.domain.exception.DomainException("Failed to read JAR: " + e.getMessage(), e) {};
        }
    }

    @Override
    public List<String> listJarEntries(String jarPath) {
        List<String> classes = new ArrayList<>();
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarPath))) {
            for (var it = jarFile.entries().asIterator(); it.hasNext(); ) {
                var entry = it.next();
                if (entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/")) {
                    String fqcn = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                    classes.add(fqcn);
                }
            }
            Collections.sort(classes);
            return classes;
        } catch (java.io.IOException e) {
            throw new com.antigravity.bytebuddy.mcp.domain.exception.DomainException("Failed to read JAR: " + e.getMessage(), e) {};
        }
    }

    @Override
    public List<ClassSummaryResponse> listClasses() {
        return repositoryPort.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public boolean removeClass(String className) {
        return repositoryPort.delete(ClassName.of(className));
    }

    @Override
    public void clearSession() {
        repositoryPort.clear();
        log.info("Session classes cleared");
    }

    private ClassSummaryResponse toSummary(BytecodeClass bc) {
        return new ClassSummaryResponse(
                bc.getName().getValue(),
                bc.getBytecodeSize(),
                bc.getMajorVersion(),
                bc.getStructure().getMethods().size(),
                bc.getStructure().getFields().size(),
                bc.getUpdatedAt()
        );
    }
}
