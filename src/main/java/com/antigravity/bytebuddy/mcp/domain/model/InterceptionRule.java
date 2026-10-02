package com.antigravity.bytebuddy.mcp.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object specifying a bytecode transformation / advice rule.
 */
public final class InterceptionRule implements Serializable {

    public enum AdviceType {
        MEASURE_EXECUTION_TIME,
        LOG_CALL_AND_RETURN,
        OVERRIDE_RETURN_VALUE,
        SUPPRESS_EXCEPTIONS
    }

    private final String methodPattern;
    private final AdviceType adviceType;
    private final Object overrideValue;

    public InterceptionRule(String methodPattern, AdviceType adviceType, Object overrideValue) {
        if (methodPattern == null || methodPattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Method pattern cannot be null or empty");
        }
        if (adviceType == null) {
            throw new IllegalArgumentException("Advice type cannot be null");
        }
        this.methodPattern = methodPattern.trim();
        this.adviceType = adviceType;
        this.overrideValue = overrideValue;
    }

    public static InterceptionRule measureTime(String methodPattern) {
        return new InterceptionRule(methodPattern, AdviceType.MEASURE_EXECUTION_TIME, null);
    }

    public static InterceptionRule logCall(String methodPattern) {
        return new InterceptionRule(methodPattern, AdviceType.LOG_CALL_AND_RETURN, null);
    }

    public static InterceptionRule overrideReturn(String methodPattern, Object value) {
        return new InterceptionRule(methodPattern, AdviceType.OVERRIDE_RETURN_VALUE, value);
    }

    public static InterceptionRule suppressExceptions(String methodPattern) {
        return new InterceptionRule(methodPattern, AdviceType.SUPPRESS_EXCEPTIONS, null);
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public AdviceType getAdviceType() {
        return adviceType;
    }

    public Object getOverrideValue() {
        return overrideValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterceptionRule that = (InterceptionRule) o;
        return Objects.equals(methodPattern, that.methodPattern) &&
                adviceType == that.adviceType &&
                Objects.equals(overrideValue, that.overrideValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodPattern, adviceType, overrideValue);
    }

    @Override
    public String toString() {
        return "InterceptionRule{" +
                "pattern='" + methodPattern + '\'' +
                ", advice=" + adviceType +
                (overrideValue != null ? ", override=" + overrideValue : "") +
                '}';
    }
}
