package com.antigravity.bytebuddy.mcp.infrastructure.adapter.persistence;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryClassRepositoryAdapterTest {

    @Test
    @DisplayName("Repository should bound heap usage via LRU eviction when max capacity is reached")
    void testLruEvictionPreventsHeapLeak() {
        int maxCapacity = 5;
        InMemoryClassRepositoryAdapter repository = new InMemoryClassRepositoryAdapter(maxCapacity);
        byte[] dummyBytecode = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 61};

        // Add 10 classes
        for (int i = 0; i < 10; i++) {
            ClassName name = ClassName.of("com.example.Class" + i);
            repository.save(BytecodeClass.of(name, dummyBytecode, 61, ClassStructure.empty("java.lang.Object")));
        }

        // Must strictly not exceed maxCapacity (5)
        assertThat(repository.count()).isEqualTo(maxCapacity);

        // Oldest classes (0 to 4) should have been evicted
        assertThat(repository.findByName(ClassName.of("com.example.Class0"))).isEmpty();
        assertThat(repository.findByName(ClassName.of("com.example.Class4"))).isEmpty();

        // Newest classes (5 to 9) should still exist
        assertThat(repository.findByName(ClassName.of("com.example.Class9"))).isPresent();

        // Clear session should empty the store
        repository.clear();
        assertThat(repository.count()).isZero();
    }
}
