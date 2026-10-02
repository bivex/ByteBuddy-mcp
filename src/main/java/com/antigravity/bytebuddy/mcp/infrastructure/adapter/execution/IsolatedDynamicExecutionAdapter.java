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
                    var ctor = loadedClass.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    targetInstance = ctor.newInstance();
                }

                Object[] invocationArgs = (args != null && args.length > 0) ? args : new Object[0];
                Object result = targetMethod.invoke(targetInstance, invocationArgs);

                long duration = System.currentTimeMillis() - start;
                return ExecutionResult.success(result, duration, capturedOutput.toString(StandardCharsets.UTF_8));
            } catch (Throwable t) {
                long duration = System.currentTimeMillis() - start;
                Throwable cause = t.getCause() != null ? t.getCause() : t;
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
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        return null;
    }
}
