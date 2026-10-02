package com.antigravity.bytebuddy.mcp.application.service;

import com.antigravity.bytebuddy.mcp.application.dto.ExecuteSnippetCommand;
import com.antigravity.bytebuddy.mcp.application.dto.SnippetExecutionResponse;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import com.antigravity.bytebuddy.mcp.domain.port.in.ExecuteSnippetUseCase;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.ClassRepositoryPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.SnippetCompilerPort;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecuteSnippetService implements ExecuteSnippetUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExecuteSnippetService.class);
    private static final AtomicInteger SNIPPET_COUNTER = new AtomicInteger(1);
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("(?:public\\s+)?(?:final\\s+)?class\\s+([A-Za-z0-9_$]+)");

    private final SnippetCompilerPort compilerPort;
    private final BytecodeDisassemblerPort disassemblerPort;
    private final ClassRepositoryPort repositoryPort;

    public ExecuteSnippetService(SnippetCompilerPort compilerPort,
                                 BytecodeDisassemblerPort disassemblerPort,
                                 ClassRepositoryPort repositoryPort) {
        this.compilerPort = Objects.requireNonNull(compilerPort);
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
        this.repositoryPort = Objects.requireNonNull(repositoryPort);
    }

    @Override
    public SnippetExecutionResponse executeSnippet(ExecuteSnippetCommand command) {
        String rawSource = command.sourceCode().trim();
        String fullSource;
        String targetClassName;

        boolean isFullClass = rawSource.contains("class ");
        if (isFullClass) {
            Matcher m = CLASS_NAME_PATTERN.matcher(rawSource);
            if (m.find()) {
                targetClassName = m.group(1);
            } else if (command.className() != null && !command.className().isBlank()) {
                targetClassName = command.className().trim();
            } else {
                targetClassName = "DynamicSnippetClass" + SNIPPET_COUNTER.getAndIncrement();
            }
            fullSource = rawSource;
        } else {
            targetClassName = "SnippetRunner" + SNIPPET_COUNTER.getAndIncrement();
            fullSource = wrapInTemplate(targetClassName, rawSource);
        }

        log.info("Compiling snippet as class '{}'", targetClassName);
        long start = System.currentTimeMillis();
        List<String> registeredClasses = new ArrayList<>();

        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream redirectOut = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8);

        try {
            Map<String, byte[]> compiledClasses = compilerPort.compile(targetClassName, fullSource);

            // Register compiled classes in session repository
            for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
                String cName = entry.getKey();
                byte[] bytes = entry.getValue();
                ClassStructure structure = disassemblerPort.parseStructure(bytes);
                int version = disassemblerPort.extractMajorVersion(bytes);
                repositoryPort.save(BytecodeClass.of(ClassName.of(cName), bytes, version, structure));
                registeredClasses.add(cName);
            }

            // Prepare isolated classloader linking against all session classes
            Map<String, byte[]> typeMap = new HashMap<>(compiledClasses);
            for (BytecodeClass bc : repositoryPort.findAll()) {
                typeMap.putIfAbsent(bc.getName().getValue(), bc.getBytecode());
            }

            ClassLoader loader = new ByteArrayClassLoader(
                    getClass().getClassLoader(),
                    typeMap,
                    ByteArrayClassLoader.PersistenceHandler.MANIFEST
            );

            Class<?> clazz = loader.loadClass(targetClassName);
            Method entryMethod = findEntryMethod(clazz);

            System.setOut(redirectOut);

            Object instance = null;
            if (!Modifier.isStatic(entryMethod.getModifiers())) {
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                instance = ctor.newInstance();
            }

            Object result;
            if (entryMethod.getParameterCount() == 1 && entryMethod.getParameterTypes()[0].isArray()) {
                result = entryMethod.invoke(instance, (Object) new String[0]);
            } else {
                result = entryMethod.invoke(instance);
            }

            // If the snippet returned a ByteBuddy DynamicType, automatically register it in repository!
            if (result instanceof DynamicType.Unloaded<?> unloaded) {
                byte[] bytes = unloaded.getBytes();
                String dynName = unloaded.getTypeDescription().getName();
                ClassStructure struct = disassemblerPort.parseStructure(bytes);
                int ver = disassemblerPort.extractMajorVersion(bytes);
                repositoryPort.save(BytecodeClass.of(ClassName.of(dynName), bytes, ver, struct));
                registeredClasses.add(dynName);
                log.info("Automatically registered ByteBuddy generated class '{}' from snippet return", dynName);
            } else if (result instanceof DynamicType.Loaded<?> loaded) {
                byte[] bytes = loaded.getBytes();
                String dynName = loaded.getLoaded().getName();
                ClassStructure struct = disassemblerPort.parseStructure(bytes);
                int ver = disassemblerPort.extractMajorVersion(bytes);
                repositoryPort.save(BytecodeClass.of(ClassName.of(dynName), bytes, ver, struct));
                registeredClasses.add(dynName);
                log.info("Automatically registered ByteBuddy loaded class '{}' from snippet return", dynName);
            }

            long duration = System.currentTimeMillis() - start;
            String outStr = capturedOutput.toString(StandardCharsets.UTF_8).trim();

            return new SnippetExecutionResponse(
                    true,
                    result != null ? result.toString() : "null",
                    duration,
                    outStr,
                    registeredClasses,
                    null
            );
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            log.warn("Snippet execution failed", cause);
            return new SnippetExecutionResponse(
                    false,
                    null,
                    duration,
                    capturedOutput.toString(StandardCharsets.UTF_8).trim(),
                    registeredClasses,
                    cause.getMessage() != null ? cause.getMessage() : cause.toString()
            );
        } finally {
            System.setOut(originalOut);
        }
    }

    private Method findEntryMethod(Class<?> clazz) throws NoSuchMethodException {
        // 1. Look for execute()
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals("execute") && m.getParameterCount() == 0) {
                m.setAccessible(true);
                return m;
            }
        }
        // 2. Look for main(String[])
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals("main") && Modifier.isStatic(m.getModifiers())) {
                m.setAccessible(true);
                return m;
            }
        }
        // 3. Look for run()
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals("run") && m.getParameterCount() == 0) {
                m.setAccessible(true);
                return m;
            }
        }
        // 4. Any public no-arg method
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && !m.getDeclaringClass().equals(Object.class)) {
                return m;
            }
        }
        throw new NoSuchMethodException("No entry method (execute(), run(), main(String[]), or public no-arg method) found in " + clazz.getName());
    }

    private String wrapInTemplate(String className, String snippet) {
        return """
                import net.bytebuddy.ByteBuddy;
                import net.bytebuddy.ClassFileVersion;
                import net.bytebuddy.dynamic.DynamicType;
                import net.bytebuddy.implementation.FixedValue;
                import net.bytebuddy.implementation.MethodDelegation;
                import net.bytebuddy.implementation.SuperMethodCall;
                import net.bytebuddy.implementation.StubMethod;
                import net.bytebuddy.implementation.ExceptionMethod;
                import net.bytebuddy.matcher.ElementMatchers;
                import static net.bytebuddy.matcher.ElementMatchers.*;
                import net.bytebuddy.asm.Advice;
                import java.lang.reflect.*;
                import java.util.*;

                public class %s {
                    public static Object execute() throws Exception {
                        %s
                    }
                }
                """.formatted(className, snippet.endsWith(";") ? snippet : snippet + ";");
    }
}
