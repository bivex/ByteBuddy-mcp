package com.antigravity.bytebuddy.mcp.domain.model;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value Object describing the structure and metadata of a class.
 */
public final class ClassStructure implements Serializable {
    private final String superClassName;
    private final List<String> interfaces;
    private final List<FieldDefinition> fields;
    private final List<MethodDefinition> methods;
    private final int modifiers;

    public ClassStructure(String superClassName,
                          List<String> interfaces,
                          List<FieldDefinition> fields,
                          List<MethodDefinition> methods,
                          int modifiers) {
        this.superClassName = (superClassName == null || superClassName.isBlank()) ? "java.lang.Object" : superClassName.trim();
        this.interfaces = interfaces == null ? Collections.emptyList() : List.copyOf(interfaces);
        this.fields = fields == null ? Collections.emptyList() : List.copyOf(fields);
        this.methods = methods == null ? Collections.emptyList() : List.copyOf(methods);
        this.modifiers = modifiers;
    }

    public static ClassStructure empty(String superClassName) {
        return new ClassStructure(superClassName, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Modifier.PUBLIC);
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public List<MethodDefinition> getMethods() {
        return methods;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isInterface() {
        return Modifier.isInterface(modifiers);
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(modifiers);
    }

    public boolean isFinal() {
        return Modifier.isFinal(modifiers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassStructure that = (ClassStructure) o;
        return modifiers == that.modifiers &&
                Objects.equals(superClassName, that.superClassName) &&
                Objects.equals(interfaces, that.interfaces) &&
                Objects.equals(fields, that.fields) &&
                Objects.equals(methods, that.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(superClassName, interfaces, fields, methods, modifiers);
    }
}
