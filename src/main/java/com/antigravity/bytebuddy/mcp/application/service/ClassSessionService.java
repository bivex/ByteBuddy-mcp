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

import java.util.Base64;
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
    public List<ClassSummaryResponse> listClasses() {
        return repositoryPort.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public boolean removeClass(String className) {
        return repositoryPort.delete(ClassName.of(className));
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
