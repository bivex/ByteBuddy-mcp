package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.InspectCommand;
import com.antigravity.bytebuddy.mcp.application.dto.InspectionResponse;
import com.antigravity.bytebuddy.mcp.domain.exception.ClassNotFoundDomainException;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import com.antigravity.bytebuddy.mcp.domain.port.in.InspectBytecodeUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

public class InspectBytecodeService implements InspectBytecodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(InspectBytecodeService.class);

    private final BytecodeDisassemblerPort disassemblerPort;
    private final ClassRepositoryPort repositoryPort;

    public InspectBytecodeService(BytecodeDisassemblerPort disassemblerPort,
                                 ClassRepositoryPort repositoryPort) {
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
    }

    @Override
    public InspectionResponse inspect(InspectCommand command) {
        byte[] bytes;
        String name;

        if (command.bytecodeBase64() != null && !command.bytecodeBase64().isBlank()) {
            bytes = Base64.getDecoder().decode(command.bytecodeBase64().trim());
            name = (command.className() != null && !command.className().isBlank())
                    ? command.className().trim()
                    : "InspectedClass";
        } else if (command.className() != null && !command.className().isBlank()) {
            ClassName className = ClassName.of(command.className());
            BytecodeClass stored = repositoryPort.findByName(className)
                    .orElseThrow(() -> new ClassNotFoundDomainException(command.className()));
            bytes = stored.getBytecode();
            name = stored.getName().getValue();
        } else {
            throw new IllegalArgumentException("Either className or bytecodeBase64 must be provided");
        }

        log.info("Inspecting structure of class '{}'", name);
        ClassStructure structure = disassemblerPort.parseStructure(bytes);
        int majorVersion = disassemblerPort.extractMajorVersion(bytes);

        return new InspectionResponse(
                name,
                bytes.length,
                majorVersion,
                structure.getSuperClassName(),
                structure.getInterfaces(),
                structure.getFields().stream().map(Object::toString).toList(),
                structure.getMethods().stream().map(Object::toString).toList(),
                structure.isInterface(),
                structure.isAbstract(),
                structure.isFinal()
        );
    }
}
