package com.antigravity.bytebuddy.mcp.infrastructure.adapter.asm;

import com.antigravity.bytebuddy.mcp.domain.model.ClassStructure;
import com.antigravity.bytebuddy.mcp.domain.model.FieldDefinition;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;
import com.antigravity.bytebuddy.mcp.domain.port.out.BytecodeDisassemblerPort;
import org.benf.cfr.reader.api.CfrDriver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Secondary adapter leveraging OW2 ASM for low-level opcode disassembly and metadata parsing,
 * and CFR for high-level Java decompilation.
 */
public class AsmAndCfrDisassemblerAdapter implements BytecodeDisassemblerPort {
    private static final Logger log = LoggerFactory.getLogger(AsmAndCfrDisassemblerAdapter.class);

    @Override
    public String disassembleToOpcodes(byte[] bytecode) {
        if (bytecode == null || bytecode.length == 0) {
            return "// Empty bytecode";
        }
        try {
            ClassReader classReader = new ClassReader(bytecode);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            TraceClassVisitor traceVisitor = new TraceClassVisitor(null, new Textifier(), printWriter);
            classReader.accept(traceVisitor, 0);
            return stringWriter.toString();
        } catch (Exception e) {
            log.error("Failed to disassemble bytecode with ASM", e);
            return "// Disassembly error: " + e.getMessage();
        }
    }

    @Override
    public String decompileToJava(byte[] bytecode, String className) {
        if (bytecode == null || bytecode.length == 0) {
            return "// No bytecode provided";
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("decomp_", ".class");
            tempFile.toFile().deleteOnExit();
            Files.write(tempFile, bytecode);

            StringBuilder decompiledResult = new StringBuilder();
            org.benf.cfr.reader.api.OutputSinkFactory sinkFactory = new org.benf.cfr.reader.api.OutputSinkFactory() {
                @Override
                public List<SinkClass> getSupportedSinks(SinkType sinkType, java.util.Collection<SinkClass> available) {
                    return Collections.singletonList(SinkClass.STRING);
                }

                @Override
                public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                    if (sinkType == SinkType.JAVA) {
                        return message -> decompiledResult.append(message);
                    }
                    return ignored -> {};
                }
            };

            Map<String, String> options = new HashMap<>();
            options.put("showversion", "false");
            options.put("comments", "false");

            CfrDriver driver = new CfrDriver.Builder()
                    .withOutputSink(sinkFactory)
                    .withOptions(options)
                    .build();

            driver.analyse(Collections.singletonList(tempFile.toAbsolutePath().toString()));
            return decompiledResult.toString().trim();
        } catch (Exception e) {
            log.warn("Decompilation with CFR encountered an issue: {}", e.getMessage());
            return "// Decompilation notice: " + e.getMessage();
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public ClassStructure parseStructure(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        String superName = cn.superName != null ? cn.superName.replace('/', '.') : "java.lang.Object";
        List<String> interfaces = cn.interfaces != null
                ? cn.interfaces.stream().map(i -> i.replace('/', '.')).toList()
                : Collections.emptyList();

        List<FieldDefinition> fields = new ArrayList<>();
        if (cn.fields != null) {
            for (FieldNode fn : cn.fields) {
                String typeName = Type.getType(fn.desc).getClassName();
                fields.add(FieldDefinition.of(fn.name, typeName, fn.access, fn.value));
            }
        }

        List<MethodDefinition> methods = new ArrayList<>();
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                Type methodType = Type.getMethodType(mn.desc);
                String returnType = methodType.getReturnType().getClassName();
                List<String> paramTypes = Arrays.stream(methodType.getArgumentTypes())
                        .map(Type::getClassName)
                        .toList();
                methods.add(new MethodDefinition(
                        mn.name,
                        returnType,
                        paramTypes,
                        mn.access,
                        MethodDefinition.Strategy.CALL_SUPER,
                        null
                ));
            }
        }

        return new ClassStructure(superName, interfaces, fields, methods, cn.access);
    }

    @Override
    public int extractMajorVersion(byte[] bytecode) {
        if (bytecode == null || bytecode.length < 8) {
            return 0;
        }
        return ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
    }
}
