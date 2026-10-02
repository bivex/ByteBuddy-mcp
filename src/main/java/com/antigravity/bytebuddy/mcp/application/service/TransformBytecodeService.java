package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.TransformBytecodeCommand;
import com.antigravity.bytebuddy.mcp.application.dto.TransformBytecodeResponse;
import com.antigravity.bytebuddy.mcp.domain.exception.ClassNotFoundDomainException;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.port.in.TransformBytecodeUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeEnginePort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

public class TransformBytecodeService implements TransformBytecodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(TransformBytecodeService.class);

    private final BytecodeEnginePort bytecodeEnginePort;
    private final BytecodeDisassemblerPort disassemblerPort;
    private final ClassRepositoryPort repositoryPort;

    public TransformBytecodeService(BytecodeEnginePort bytecodeEnginePort,
                                   BytecodeDisassemblerPort disassemblerPort,
                                   ClassRepositoryPort repositoryPort) {
        this.bytecodeEnginePort = Objects.requireNonNull(bytecodeEnginePort);
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
    }

    @Override
    public TransformBytecodeResponse transform(TransformBytecodeCommand command) {
        BytecodeClass targetClass;
        ClassName targetName = null;

        if (command.className() != null && !command.className().isBlank()) {
            targetName = ClassName.of(command.className());
            targetClass = repositoryPort.findByName(targetName).orElse(null);
        } else {
            targetClass = null;
        }

        if (targetClass == null && command.bytecodeBase64() != null && !command.bytecodeBase64().isBlank()) {
            byte[] bytes = Base64.getDecoder().decode(command.bytecodeBase64().trim());
            var structure = disassemblerPort.parseStructure(bytes);
            int version = disassemblerPort.extractMajorVersion(bytes);
            if (targetName == null) {
                targetName = ClassName.of(structure.getSuperClassName().equals("java.lang.Object") ? "DynamicTransformed" : "DynamicClass");
            }
            targetClass = BytecodeClass.of(targetName, bytes, version, structure);
        }

        if (targetClass == null) {
            throw new ClassNotFoundDomainException(command.className() != null ? command.className() : "unknown");
        }

        int originalSize = targetClass.getBytecodeSize();
        log.info("Transforming class '{}' with {} interception rules", targetClass.getName(), command.rules().size());

        BytecodeClass transformed = bytecodeEnginePort.transformBytecode(targetClass, command.rules());
        repositoryPort.save(transformed);

        String decompiled = disassemblerPort.decompileToJava(transformed.getBytecode(), transformed.getName().getValue());
        String base64 = Base64.getEncoder().encodeToString(transformed.getBytecode());

        return new TransformBytecodeResponse(
                transformed.getName().getValue(),
                originalSize,
                transformed.getBytecodeSize(),
                command.rules().stream().map(Object::toString).toList(),
                decompiled,
                base64
        );
    }
}
