package com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe LRU in-memory adapter for storing generated and loaded BytecodeClass aggregates.
 * Automatically evicts least recently accessed classes when maximum capacity is exceeded to prevent heap exhaustion.
 */
public class InMemoryClassRepositoryAdapter implements ClassRepositoryPort {
    private static final Logger log = LoggerFactory.getLogger(InMemoryClassRepositoryAdapter.class);
    private static final int DEFAULT_MAX_CAPACITY = 1000;

    private final int maxCapacity;
    private final Map<ClassName, BytecodeClass> store;

    public InMemoryClassRepositoryAdapter() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public InMemoryClassRepositoryAdapter(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.store = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ClassName, BytecodeClass> eldest) {
                if (size() > InMemoryClassRepositoryAdapter.this.maxCapacity) {
                    log.info("LRU evicting oldest class '{}' to maintain capacity bound of {}", eldest.getKey(), maxCapacity);
                    return true;
                }
                return false;
            }
        });
    }

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
        synchronized (store) {
            return new ArrayList<>(store.values());
        }
    }

    @Override
    public boolean delete(ClassName className) {
        return store.remove(className) != null;
    }

    @Override
    public void clear() {
        store.clear();
        log.info("Cleared all stored bytecode classes from session repository");
    }

    @Override
    public int count() {
        return store.size();
    }
}
