package com.antigravity.bytebuddy.mcp.domain.model;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value Object representing a method definition to generate or match.
 */
public final class MethodDefinition implements Serializable {

    public enum Strategy {
        RETURN_CONSTANT,
        RETURN_DEFAULT,
        CALL_SUPER,
        THROW_EXCEPTION
    }

    private final String name;
    private final String returnType;
    private final List<String> parameterTypes;
    private final int modifiers;
    private final Strategy strategy;
    private final Object constantValue;

    public MethodDefinition(String name,
                            String returnType,
                            List<String> parameterTypes,
                            int modifiers,
                            Strategy strategy,
                            Object constantValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Method name cannot be null or empty");
        }
        this.name = name.trim();
        this.returnType = (returnType == null || returnType.trim().isEmpty()) ? "void" : returnType.trim();
        this.parameterTypes = parameterTypes == null ? Collections.emptyList() : List.copyOf(parameterTypes);
        this.modifiers = modifiers;
        this.strategy = strategy == null ? Strategy.RETURN_DEFAULT : strategy;
        this.constantValue = constantValue;
    }

    public static MethodDefinition returningConstant(String name, String returnType, Object value) {
        return new MethodDefinition(name, returnType, Collections.emptyList(), Modifier.PUBLIC, Strategy.RETURN_CONSTANT, value);
    }

    public static MethodDefinition returningDefault(String name, String returnType, List<String> paramTypes) {
        return new MethodDefinition(name, returnType, paramTypes, Modifier.PUBLIC, Strategy.RETURN_DEFAULT, null);
    }

    public static MethodDefinition throwing(String name, String returnType, String exceptionMessage) {
        return new MethodDefinition(name, returnType, Collections.emptyList(), Modifier.PUBLIC, Strategy.THROW_EXCEPTION, exceptionMessage);
    }

    public static MethodDefinition callingSuper(String name, String returnType, List<String> paramTypes) {
        return new MethodDefinition(name, returnType, paramTypes, Modifier.PUBLIC, Strategy.CALL_SUPER, null);
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public Object getConstantValue() {
        return constantValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodDefinition that = (MethodDefinition) o;
        return modifiers == that.modifiers &&
                Objects.equals(name, that.name) &&
                Objects.equals(returnType, that.returnType) &&
                Objects.equals(parameterTypes, that.parameterTypes) &&
                strategy == that.strategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, parameterTypes, modifiers, strategy);
    }

    @Override
    public String toString() {
        return Modifier.toString(modifiers) + " " + returnType + " " + name + "(" + String.join(", ", parameterTypes) + ")";
    }
}
