package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.GenerateClassCommand;
import com.antigravity.bytebuddy.mcp.application.dto.GeneratedClassResponse;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.port.in.GenerateClassUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeEnginePort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

public class GenerateClassService implements GenerateClassUseCase {
    private static final Logger log = LoggerFactory.getLogger(GenerateClassService.class);

    private final BytecodeEnginePort bytecodeEnginePort;
    private final BytecodeDisassemblerPort disassemblerPort;
    private final ClassRepositoryPort repositoryPort;

    public GenerateClassService(BytecodeEnginePort bytecodeEnginePort,
                                BytecodeDisassemblerPort disassemblerPort,
                                ClassRepositoryPort repositoryPort) {
        this.bytecodeEnginePort = Objects.requireNonNull(bytecodeEnginePort);
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
    }

    @Override
    public GeneratedClassResponse generateClass(GenerateClassCommand command) {
        ClassName className = ClassName.of(command.className());
        log.info("Generating dynamic class '{}' with {} fields and {} methods",
                className, command.fields().size(), command.methods().size());

        BytecodeClass generated = bytecodeEnginePort.generateClass(
                className,
                command.superClassName(),
                command.interfaces(),
                command.fields(),
                command.methods()
        );

        repositoryPort.save(generated);

        String decompiled = disassemblerPort.decompileToJava(generated.getBytecode(), className.getValue());
        String base64 = Base64.getEncoder().encodeToString(generated.getBytecode());

        return new GeneratedClassResponse(
                className.getValue(),
                generated.getBytecodeSize(),
                generated.getMajorVersion(),
                generated.getStructure().getSuperClassName(),
                generated.getStructure().getInterfaces(),
                generated.getStructure().getFields().stream().map(Object::toString).toList(),
                generated.getStructure().getMethods().stream().map(Object::toString).toList(),
                decompiled,
                base64
        );
    }
}
