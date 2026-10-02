package com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy;

import com.antigravity.bytebuddy.mcp.domain.exception.BytecodeGenerationException;
import com.antigravity.bytebuddy.mcp.domain.exception.BytecodeTransformationException;
import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import com.antigravity.bytebuddy.mcp.domain.model.FieldDefinition;
import com.antigravity.bytebuddy.mcp.domain.model.InterceptionRule;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeEnginePort;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Secondary adapter implementing BytecodeEnginePort via ByteBuddy.
 */
public class ByteBuddyEngineAdapter implements BytecodeEnginePort {
    private static final Logger log = LoggerFactory.getLogger(ByteBuddyEngineAdapter.class);

    static {
        // Enable ByteBuddy experimental support on early-access or newer JVM versions (e.g. Java 25/26+)
        if (System.getProperty("net.bytebuddy.experimental") == null) {
            System.setProperty("net.bytebuddy.experimental", "true");
        }
    }

    private static final Map<String, Class<?>> PRIMITIVE_TYPES = Map.ofEntries(
            Map.entry("void", void.class),
            Map.entry("int", int.class),
            Map.entry("long", long.class),
            Map.entry("boolean", boolean.class),
            Map.entry("double", double.class),
            Map.entry("float", float.class),
            Map.entry("byte", byte.class),
            Map.entry("short", short.class),
            Map.entry("char", char.class),
            Map.entry("String", String.class),
            Map.entry("Object", Object.class)
    );

    private final ByteBuddy byteBuddy;
    private final BytecodeDisassemblerPort disassemblerPort;

    public ByteBuddyEngineAdapter(BytecodeDisassemblerPort disassemblerPort) {
        this(disassemblerPort, net.bytebuddy.ClassFileVersion.JAVA_V17);
    }

    public ByteBuddyEngineAdapter(BytecodeDisassemblerPort disassemblerPort, net.bytebuddy.ClassFileVersion classFileVersion) {
        this.byteBuddy = new ByteBuddy(classFileVersion != null ? classFileVersion : net.bytebuddy.ClassFileVersion.JAVA_V17);
        this.disassemblerPort = Objects.requireNonNull(disassemblerPort);
    }

    @Override
    public BytecodeClass generateClass(ClassName name,
                                       String superClassName,
                                       List<String> interfaces,
                                       List<FieldDefinition> fields,
                                       List<MethodDefinition> methods) {
        try {
            Class<?> superClass = resolveClass(superClassName, Object.class);
            List<Class<?>> interfaceClasses = new ArrayList<>();
            if (interfaces != null) {
                for (String iface : interfaces) {
                    interfaceClasses.add(resolveClass(iface, null));
                }
            }

            DynamicType.Builder<?> builder = byteBuddy
                    .subclass(superClass)
                    .name(name.getValue());

            if (!interfaceClasses.isEmpty()) {
                builder = builder.implement(interfaceClasses);
            }

            if (fields != null) {
                for (FieldDefinition field : fields) {
                    Class<?> fieldType = resolveType(field.getType());
                    builder = builder.defineField(field.getName(), fieldType, field.getModifiers());
                }
            }

            if (methods != null) {
                for (MethodDefinition method : methods) {
                    Class<?> returnType = resolveType(method.getReturnType());
                    List<Class<?>> paramTypes = method.getParameterTypes().stream()
                            .map(this::resolveType)
                            .toList();

                    Implementation impl = determineImplementation(method);

                    builder = builder.defineMethod(method.getName(), returnType, method.getModifiers())
                            .withParameters(paramTypes)
                            .intercept(impl);
                }
            }

            DynamicType.Unloaded<?> unloaded = builder.make();
            byte[] bytecode = unloaded.getBytes();

            ClassStructure structure = disassemblerPort.parseStructure(bytecode);
            int version = disassemblerPort.extractMajorVersion(bytecode);

            return BytecodeClass.of(name, bytecode, version, structure);
        } catch (Exception e) {
            log.error("Failed to generate dynamic class with ByteBuddy", e);
            throw new BytecodeGenerationException("Failed to generate class " + name + ": " + e.getMessage(), e);
        }
    }

    @Override
    public BytecodeClass transformBytecode(BytecodeClass target, List<InterceptionRule> rules) {
        try {
            String className = target.getName().getValue();
            byte[] initialBytecode = target.getBytecode();

            ClassFileLocator locator = new ClassFileLocator.Compound(
                    ClassFileLocator.Simple.of(className, initialBytecode),
                    ClassFileLocator.ForClassLoader.ofSystemLoader()
            );
            TypeDescription typeDescription = TypePool.Default.of(locator).describe(className).resolve();

            DynamicType.Builder<?> builder = byteBuddy.redefine(typeDescription, locator);

            for (InterceptionRule rule : rules) {
                var matcher = ElementMatchers.nameMatches(rule.getMethodPattern());

                switch (rule.getAdviceType()) {
                    case MEASURE_EXECUTION_TIME ->
                            builder = builder.visit(Advice.to(ByteBuddyAdviceTemplates.ExecutionTimeAdvice.class).on(matcher));
                    case LOG_CALL_AND_RETURN ->
                            builder = builder.visit(Advice.to(ByteBuddyAdviceTemplates.LoggingAdvice.class).on(matcher));
                    case SUPPRESS_EXCEPTIONS ->
                            builder = builder.visit(Advice.to(ByteBuddyAdviceTemplates.ExceptionSuppressionAdvice.class).on(matcher));
                    case OVERRIDE_RETURN_VALUE -> {
                        Object val = rule.getOverrideValue();
                        Implementation fixedVal = (val == null) ? FixedValue.nullValue() : FixedValue.value(val);
                        builder = builder.method(matcher).intercept(fixedVal);
                    }
                }
            }

            DynamicType.Unloaded<?> unloaded = builder.make();
            byte[] transformedBytecode = unloaded.getBytes();

            ClassStructure structure = disassemblerPort.parseStructure(transformedBytecode);
            int version = disassemblerPort.extractMajorVersion(transformedBytecode);

            target.updateBytecode(transformedBytecode, structure);
            return target;
        } catch (Exception e) {
            log.error("Failed to transform bytecode with ByteBuddy", e);
            throw new BytecodeTransformationException("Failed to transform class " + target.getName() + ": " + e.getMessage(), e);
        }
    }

    private Implementation determineImplementation(MethodDefinition method) {
        return switch (method.getStrategy()) {
            case RETURN_CONSTANT -> {
                Object val = method.getConstantValue();
                if (val == null) yield FixedValue.nullValue();
                yield FixedValue.value(val);
            }
            case CALL_SUPER -> SuperMethodCall.INSTANCE;
            case THROW_EXCEPTION -> {
                String msg = method.getConstantValue() != null ? method.getConstantValue().toString() : "Exception from dynamic method";
                yield ExceptionMethod.throwing(RuntimeException.class, msg);
            }
            case RETURN_DEFAULT -> StubMethod.INSTANCE;
        };
    }

    private Class<?> resolveType(String typeName) {
        if (PRIMITIVE_TYPES.containsKey(typeName)) {
            return PRIMITIVE_TYPES.get(typeName);
        }
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("java.lang." + typeName);
            } catch (ClassNotFoundException ignored) {
                return Object.class;
            }
        }
    }

    private Class<?> resolveClass(String className, Class<?> defaultClass) {
        if (className == null || className.isBlank()) {
            return defaultClass;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            if (defaultClass != null) {
                return defaultClass;
            }
            throw new IllegalArgumentException("Cannot resolve interface or class: " + className, e);
        }
    }
}
