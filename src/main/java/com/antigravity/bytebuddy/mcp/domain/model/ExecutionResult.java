package com.antigravity.bytebuddy.mcp.domain.model;

import java.io.Serializable;

/**
 * Value Object representing the outcome of a dynamic method invocation.
 */
public final class ExecutionResult implements Serializable {
    private final boolean success;
    private final Object returnValue;
    private final long executionDurationMs;
    private final String capturedOutput;
    private final String errorMessage;
    private final String exceptionClassName;

    private ExecutionResult(boolean success,
                            Object returnValue,
                            long executionDurationMs,
                            String capturedOutput,
                            String errorMessage,
                            String exceptionClassName) {
        this.success = success;
        this.returnValue = returnValue;
        this.executionDurationMs = executionDurationMs;
        this.capturedOutput = capturedOutput == null ? "" : capturedOutput;
        this.errorMessage = errorMessage;
        this.exceptionClassName = exceptionClassName;
    }

    public static ExecutionResult success(Object value, long durationMs, String output) {
        return new ExecutionResult(true, value, durationMs, output, null, null);
    }

    public static ExecutionResult failure(String errorMessage, String exceptionClass, long durationMs, String output) {
        return new ExecutionResult(false, null, durationMs, output, errorMessage, exceptionClass);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public String getCapturedOutput() {
        return capturedOutput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }
}
