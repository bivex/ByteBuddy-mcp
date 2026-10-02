package com.antigravity.bytebuddy.mcp.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object representing a class field definition.
 */
public final class FieldDefinition implements Serializable {
    private final String name;
    private final String type;
    private final int modifiers;
    private final Object defaultValue;

    public FieldDefinition(String name, String type, int modifiers, Object defaultValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Field type cannot be null or empty");
        }
        this.name = name.trim();
        this.type = type.trim();
        this.modifiers = modifiers;
        this.defaultValue = defaultValue;
    }

    public static FieldDefinition of(String name, String type) {
        return new FieldDefinition(name, type, java.lang.reflect.Modifier.PRIVATE, null);
    }

    public static FieldDefinition of(String name, String type, int modifiers, Object defaultValue) {
        return new FieldDefinition(name, type, modifiers, defaultValue);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldDefinition that = (FieldDefinition) o;
        return modifiers == that.modifiers &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, modifiers);
    }

    @Override
    public String toString() {
        return java.lang.reflect.Modifier.toString(modifiers) + " " + type + " " + name;
    }
}
