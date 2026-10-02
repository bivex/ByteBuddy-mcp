package com.antigravity.bytebuddy.mcp.domain.model;

import com.antigravity.bytebuddy.mcp.domain.exception.InvalidClassNameException;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a fully qualified Java class name.
 */
public final class ClassName implements Serializable, Comparable<ClassName> {
    private static final Pattern VALID_IDENTIFIER = Pattern.compile(
            "^([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*$"
    );

    private final String value;

    private ClassName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidClassNameException(value, "Class name cannot be null or blank");
        }
        String normalized = value.trim();
        if (!VALID_IDENTIFIER.matcher(normalized).matches()) {
            throw new InvalidClassNameException(normalized, "Does not match Java fully qualified identifier format");
        }
        this.value = normalized;
    }

    public static ClassName of(String value) {
        return new ClassName(value);
    }

    public String getValue() {
        return value;
    }

    public String getSimpleName() {
        int lastDot = value.lastIndexOf('.');
        return lastDot == -1 ? value : value.substring(lastDot + 1);
    }

    public String getPackageName() {
        int lastDot = value.lastIndexOf('.');
        return lastDot == -1 ? "" : value.substring(0, lastDot);
    }

    public String getInternalName() {
        return value.replace('.', '/');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassName className = (ClassName) o;
        return Objects.equals(value, className.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(ClassName o) {
        return this.value.compareTo(o.value);
    }
}
