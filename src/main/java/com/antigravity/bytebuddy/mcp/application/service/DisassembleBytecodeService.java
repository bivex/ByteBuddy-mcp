package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.DisassembleCommand;
import com.antigravity.bytebuddy.mcp.application.dto.DisassemblyResponse;
import com.antigravity.bytebuddy.mcp.domain.exception.ClassNotFoundDomainException;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.port.in.DisassembleBytecodeUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

public class DisassembleBytecodeService implements DisassembleBytecodeUseCase {
    private static final Logger log = LoggerFactory.getLogger(DisassembleBytecodeService.class);

    private final BytecodeDisassemblerPort disassemblerPort;
    private final ClassRepositoryPort repositoryPort;

    public DisassembleBytecodeService(BytecodeDisassemblerPort disassemblerPort,
                                     ClassRepositoryPort repositoryPort) {
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
    }

    @Override
    public DisassemblyResponse disassemble(DisassembleCommand command) {
        byte[] bytes;
        String name;

        if (command.bytecodeBase64() != null && !command.bytecodeBase64().isBlank()) {
            bytes = Base64.getDecoder().decode(command.bytecodeBase64().trim());
            name = (command.className() != null && !command.className().isBlank())
                    ? command.className().trim()
                    : "UnknownClass";
        } else if (command.className() != null && !command.className().isBlank()) {
            ClassName className = ClassName.of(command.className());
            BytecodeClass stored = repositoryPort.findByName(className)
                    .orElseThrow(() -> new ClassNotFoundDomainException(command.className()));
            bytes = stored.getBytecode();
            name = stored.getName().getValue();
        } else {
            throw new IllegalArgumentException("Either className or bytecodeBase64 must be provided");
        }

        log.info("Disassembling bytecode for '{}' ({} bytes)", name, bytes.length);

        String decompiled = command.includeDecompiledJava()
                ? disassemblerPort.decompileToJava(bytes, name)
                : "";

        String opcodes = command.includeOpcodes()
                ? disassemblerPort.disassembleToOpcodes(bytes)
                : "";

        return new DisassemblyResponse(name, bytes.length, decompiled, opcodes);
    }
}
