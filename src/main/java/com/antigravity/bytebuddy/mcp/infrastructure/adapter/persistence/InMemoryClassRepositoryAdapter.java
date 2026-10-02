package com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory adapter for storing generated and loaded BytecodeClass aggregates.
 */
public class InMemoryClassRepositoryAdapter implements ClassRepositoryPort {
    private final Map<ClassName, BytecodeClass> store = new ConcurrentHashMap<>();

    @Override
    public void save(BytecodeClass bytecodeClass) {
        store.put(bytecodeClass.getName(), bytecodeClass);
    }

    @Override
    public Optional<BytecodeClass> findByName(ClassName className) {
        return Optional.ofNullable(store.get(className));
    }

    @Override
    public List<BytecodeClass> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public boolean delete(ClassName className) {
        return store.remove(className) != null;
    }
}
