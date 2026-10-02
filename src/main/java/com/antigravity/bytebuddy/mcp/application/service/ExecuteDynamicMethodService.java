package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.ExecuteMethodCommand;
import com.antigravity.bytebuddy.mcp.application.dto.ExecutionResponse;
import com.antigravity.bytebuddy.mcp.domain.exception.ClassNotFoundDomainException;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;
import com.antigravity.bytebuddy.mcp.domain.port.in.ExecuteDynamicMethodUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.DynamicExecutionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ExecuteDynamicMethodService implements ExecuteDynamicMethodUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExecuteDynamicMethodService.class);

    private final DynamicExecutionPort executionPort;
    private final ClassRepositoryPort repositoryPort;

    public ExecuteDynamicMethodService(DynamicExecutionPort executionPort,
                                      ClassRepositoryPort repositoryPort) {
        this.executionPort = Objects.requireNonNull(executionPort);
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
    }

    @Override
    public ExecutionResponse executeMethod(ExecuteMethodCommand command) {
        ClassName className = ClassName.of(command.className());
        BytecodeClass targetClass = repositoryPort.findByName(className)
                .orElseThrow(() -> new ClassNotFoundDomainException(command.className()));

        log.info("Executing method '{}.{}' with {} arguments",
                className, command.methodName(), command.arguments().size());

        ExecutionResult result = executionPort.execute(
                targetClass,
                command.methodName(),
                command.arguments().toArray()
        );

        return new ExecutionResponse(
                result.isSuccess(),
                result.getReturnValue(),
                result.getExecutionDurationMs(),
                result.getCapturedOutput(),
                result.getErrorMessage(),
                result.getExceptionClassName()
        );
    }
}
