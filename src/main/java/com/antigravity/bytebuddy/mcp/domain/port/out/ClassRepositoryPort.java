package com.antigravity.bytebuddy.mcp.domain.port.out;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;

import java.util.List;
import java.util.Optional;

/**
 * Secondary (outbound) port for storing and querying loaded/generated classes in the current session.
 */
public interface ClassRepositoryPort {

    void save(BytecodeClass bytecodeClass);

    Optional<BytecodeClass> findByName(ClassName className);

    List<BytecodeClass> findAll();

    boolean delete(ClassName className);
}
