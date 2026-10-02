package com.antigravity.bytebuddy.mcp.infrastructure.adapter.compiler;

import com.antigravity.bytebuddy.mcp.domain.exception.DomainException;
import com.antigravity.bytebuddy.mcp.domain.port.out.SnippetCompilerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Secondary adapter implementing in-memory dynamic Java compilation using JDK standard ToolProvider.
 */
public class JdkInMemoryCompilerAdapter implements SnippetCompilerPort {
    private static final Logger log = LoggerFactory.getLogger(JdkInMemoryCompilerAdapter.class);

    @Override
    public Map<String, byte[]> compile(String className, String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new DomainException("JDK JavaCompiler not found on this JVM runtime. A JDK is required.") {};
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Map<String, ByteArrayOutputStream> compiledBytes = new HashMap<>();

        JavaFileManager inMemoryFileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(standardFileManager) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location,
                                                       String className,
                                                       JavaFileObject.Kind kind,
                                                       FileObject sibling) {
                return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compiledBytes.put(className, baos);
                        return baos;
                    }
                };
            }
        };

        JavaFileObject sourceFile = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sourceCode;
            }
        };

        List<String> options = new ArrayList<>();
        options.add("-source");
        options.add("17");
        options.add("-target");
        options.add("17");
        String classPath = System.getProperty("java.class.path");
        if (classPath != null && !classPath.isBlank()) {
            options.add("-classpath");
            options.add(classPath);
        }

        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                inMemoryFileManager,
                diagnostics,
                options,
                null,
                List.of(sourceFile)
        );

        boolean success = task.call();
        if (!success || compiledBytes.isEmpty()) {
            StringBuilder sb = new StringBuilder("Compilation failed:\n");
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                sb.append(String.format("Line %d: %s\n", d.getLineNumber(), d.getMessage(null)));
            }
            log.warn("Dynamic snippet compilation error: {}", sb);
            throw new DomainException(sb.toString().trim()) {};
        }

        Map<String, byte[]> result = new HashMap<>();
        for (Map.Entry<String, ByteArrayOutputStream> entry : compiledBytes.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toByteArray());
        }

        return result;
    }
}
