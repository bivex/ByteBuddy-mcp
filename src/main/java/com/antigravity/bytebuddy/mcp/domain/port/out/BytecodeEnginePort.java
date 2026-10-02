package com.antigravity.bytebuddy.mcp.domain.port.out;

import com.antigravity.bytebuddy.mcp.domain.model.BytecodeClass;
import com.antigravity.bytebuddy.mcp.domain.model.ClassName;
import com.antigravity.bytebuddy.mcp.domain.model.FieldDefinition;
import com.antigravity.bytebuddy.mcp.domain.model.InterceptionRule;
import com.antigravity.bytebuddy.mcp.domain.model.MethodDefinition;

import java.util.List;

/**
 * Secondary (outbound) port defining bytecode synthesis and transformation capabilities via ByteBuddy.
 */
public interface BytecodeEnginePort {

    BytecodeClass generateClass(ClassName name,
                                String superClassName,
                                List<String> interfaces,
                                List<FieldDefinition> fields,
                                List<MethodDefinition> methods);

    BytecodeClass transformBytecode(BytecodeClass target, List<InterceptionRule> rules);
}
