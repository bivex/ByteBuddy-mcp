package com.antigravity.bytebuddy.mcp.infrastructure.adapter.execution;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ExecutionResult;
import com.antigravity.bytebuddy.mcp.domain.port.out.DynamicExecutionPort;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Secondary adapter for dynamic method execution in an isolated ClassLoader.
 * Captures stdout and measures execution latency safely.
 */
public class IsolatedDynamicExecutionAdapter implements DynamicExecutionPort {
    private static final Logger log = LoggerFactory.getLogger(IsolatedDynamicExecutionAdapter.class);

    private final com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort repositoryPort;

    public IsolatedDynamicExecutionAdapter() {
        this(null);
    }

    public IsolatedDynamicExecutionAdapter(com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public ExecutionResult execute(BytecodeClass bytecodeClass, String methodName, Object[] args) {
        String className = bytecodeClass.getName().getValue();
        byte[] bytes = bytecodeClass.getBytecode();

        com.antigravity.bytebuddy.mcp.infrastructure.util.BoundedByteArrayOutputStream capturedOutput =
                new com.antigravity.bytebuddy.mcp.infrastructure.util.BoundedByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream redirectOut = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8);

        long start = System.currentTimeMillis();
        synchronized (System.class) {
            try {
                // Load class inside isolated ByteArrayClassLoader with all session classes
                Map<String, byte[]> typeMap = new java.util.HashMap<>();
                if (repositoryPort != null) {
                    for (BytecodeClass bc : repositoryPort.findAll()) {
                        typeMap.put(bc.getName().getValue(), bc.getBytecode());
                    }
                }
                typeMap.put(className, bytes);

                ClassLoader isolatedLoader = new ByteArrayClassLoader(
                        getClass().getClassLoader(),
                        typeMap,
                        ByteArrayClassLoader.PersistenceHandler.MANIFEST
                );

                Class<?> loadedClass = isolatedLoader.loadClass(className);

                Method targetMethod = findMethod(loadedClass, methodName, args);
                if (targetMethod == null) {
                    return ExecutionResult.failure(
                            "Method '" + methodName + "' not found on class " + className,
                            NoSuchMethodException.class.getName(),
                            0,
                            ""
                    );
                }
                targetMethod.setAccessible(true);

                // Redirect stdout temporarily
                System.setOut(redirectOut);

                Object targetInstance = null;
                if (!Modifier.isStatic(targetMethod.getModifiers())) {
                    try {
                        var ctor = loadedClass.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        targetInstance = ctor.newInstance();
                    } catch (NoSuchMethodException e) {
                        throw new IllegalStateException("Cannot invoke instance method '" + methodName +
                                "': Class '" + className + "' does not define a no-arg constructor", e);
                    }
                }

                Object[] rawArgs = (args != null && args.length > 0) ? args : new Object[0];
                Object[] invocationArgs = coerceArguments(targetMethod.getParameterTypes(), rawArgs);
                Object result = targetMethod.invoke(targetInstance, invocationArgs);

                long duration = System.currentTimeMillis() - start;
                return ExecutionResult.success(result, duration, capturedOutput.toString(StandardCharsets.UTF_8));
            } catch (Throwable t) {
                long duration = System.currentTimeMillis() - start;
                Throwable cause = (t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null)
                        ? ite.getCause()
                        : t;
                log.warn("Dynamic execution failed for '{}.{}'", className, methodName, cause);
                return ExecutionResult.failure(
                        cause.getMessage() != null ? cause.getMessage() : cause.toString(),
                        cause.getClass().getName(),
                        duration,
                        capturedOutput.toString(StandardCharsets.UTF_8)
                );
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    private Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        int argCount = args == null ? 0 : args.length;
        List<Method> candidates = new java.util.ArrayList<>();

        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == argCount) {
                candidates.add(m);
            }
        }
        if (candidates.isEmpty()) {
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == argCount) {
                    candidates.add(m);
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1 || args == null || args.length == 0) {
            return candidates.get(0);
        }
        for (Method m : candidates) {
            if (isParametersCompatible(m.getParameterTypes(), args)) {
                return m;
            }
        }
        return candidates.get(0);
    }

    private boolean isParametersCompatible(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) continue;
            Class<?> pt = paramTypes[i];
            if (pt.isPrimitive()) {
                if (pt == int.class && args[i] instanceof Number) continue;
                if (pt == long.class && args[i] instanceof Number) continue;
                if (pt == double.class && args[i] instanceof Number) continue;
                if (pt == float.class && args[i] instanceof Number) continue;
                if (pt == boolean.class && args[i] instanceof Boolean) continue;
                return false;
            } else if (!pt.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private Object[] coerceArguments(Class<?>[] paramTypes, Object[] args) {
        if (args == null || args.length == 0) return new Object[0];
        Object[] coerced = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            coerced[i] = (i < paramTypes.length) ? coerceValue(paramTypes[i], args[i]) : args[i];
        }
        return coerced;
    }

    private Object coerceValue(Class<?> targetType, Object value) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        if (value instanceof Number num) {
            if (targetType == int.class || targetType == Integer.class) return num.intValue();
            if (targetType == long.class || targetType == Long.class) return num.longValue();
            if (targetType == double.class || targetType == Double.class) return num.doubleValue();
            if (targetType == float.class || targetType == Float.class) return num.floatValue();
            if (targetType == short.class || targetType == Short.class) return num.shortValue();
            if (targetType == byte.class || targetType == Byte.class) return num.byteValue();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean b) return b;
            return Boolean.parseBoolean(value.toString());
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }
}
