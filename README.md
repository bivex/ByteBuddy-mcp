# ByteBuddy MCP Server (Hexagonal & DDD Architecture)

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Model Context Protocol](https://img.shields.io/badge/MCP-2024--11--05-green.svg)](https://modelcontextprotocol.io/)
[![Architecture: Hexagonal + DDD](https://img.shields.io/badge/Architecture-Hexagonal%20%2B%20DDD-purple.svg)](https://alistair.cockburn.us/hexagonal-architecture/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A high-performance **Model Context Protocol (MCP)** server providing AI assistants (Claude, Cursor, Antigravity, Cline) with deep runtime bytecode manipulation, synthesis, disassembly, and dynamic execution capabilities powered by **ByteBuddy**, **OW2 ASM**, and **CFR Decompiler**.

---

## 🏛️ Architecture: Hexagonal (Ports & Adapters) + DDD

The project is structured according to **Domain-Driven Design (DDD)** and **Hexagonal Architecture (Ports and Adapters)**, ensuring the domain layer is completely decoupled from protocols, I/O, and external frameworks.

```
                  +---------------------------------------------------------------+
                  |                      Driving Adapters                         |
                  |  [ Claude / Cline / Antigravity / Cursor via Stdio JSON-RPC ] |
                  +-------------------------------+-------------------------------+
                                                  |
                                                  v
                  +---------------------------------------------------------------+
                  |                   com.antigravity.bytebuddy.mcp               |
                  |                                                               |
                  |  +---------------------------------------------------------+  |
                  |  |                     Inbound Ports                       |  |
                  |  |  * GenerateClassUseCase     * TransformBytecodeUseCase  |  |
                  |  |  * DisassembleBytecodeUseCase * InspectBytecodeUseCase  |  |
                  |  |  * ExecuteDynamicMethodUseCase * ClassSessionUseCase    |  |
                  |  +----------------------------+----------------------------+  |
                  |                               |                               |
                  |                               v                               |
                  |  +---------------------------------------------------------+  |
                  |  |                    Application Layer                    |  |
                  |  |  * GenerateClassService     * TransformBytecodeService  |  |
                  |  |  * DisassembleService       * InspectService            |  |
                  |  |  * DynamicExecutionService  * ClassSessionService       |  |
                  |  +----------------------------+----------------------------+  |
                  |                               |                               |
                  |                               v                               |
                  |  +---------------------------------------------------------+  |
                  |  |                 Domain Layer (Core DDD)                 |  |
                  |  |  Aggregates: BytecodeClass                              |  |
                  |  |  Value Objects: ClassName, MethodDefinition,            |  |
                  |  |                 FieldDefinition, ClassStructure,        |  |
                  |  |                 InterceptionRule, ExecutionResult       |  |
                  |  |  Exceptions: DomainException, BytecodeGenException      |  |
                  |  +----------------------------+----------------------------+  |
                  |                               |                               |
                  |                               v                               |
                  |  +---------------------------------------------------------+  |
                  |  |                    Outbound Ports                       |  |
                  |  |  * BytecodeEnginePort       * BytecodeDisassemblerPort  |  |
                  |  |  * DynamicExecutionPort     * ClassRepositoryPort       |  |
                  |  +----------------------------+----------------------------+  |
                  +-------------------------------+-------------------------------+
                                                  |
                                                  v
                  +---------------------------------------------------------------+
                  |                      Driven Adapters                          |
                  |  * ByteBuddyEngineAdapter (ByteBuddy 1.15)                    |
                  |  * AsmAndCfrDisassemblerAdapter (ASM 9.7 & CFR Decompiler)    |
                  |  * IsolatedDynamicExecutionAdapter (ByteArrayClassLoader)     |
                  |  * InMemoryClassRepositoryAdapter (ConcurrentHashMap)         |
                  +---------------------------------------------------------------+
```

### Clean Layer Separation
1. **`domain`**: Contains pure business rules, the `BytecodeClass` Aggregate Root, Value Objects (`ClassName`, `MethodDefinition`, `InterceptionRule`), and Domain Exceptions. Zero dependencies on MCP or frameworks.
2. **`domain.port.in` & `domain.port.out`**: Contract boundaries between outside world and core domain.
3. **`application`**: Use case orchestrators (`GenerateClassService`, `TransformBytecodeService`, etc.) mapping inbound commands to domain logic and calling outbound ports.
4. **`infrastructure`**: Implementation of driven ports using **ByteBuddy**, **ASM**, **CFR**, and isolated classloaders.
5. **`adapter.mcp`**: Implementation of the driving MCP protocol adapter handling JSON-RPC 2.0 requests over standard input/output.

---

## 🛠️ MCP Tools Exposed to AI Agents

| Tool | Description | Key Inputs |
| :--- | :--- | :--- |
| `bytebuddy_generate_class` | Synthesizes a new Java class dynamically at runtime. | `className`, `superClassName`, `interfaces`, `fields`, `methods` (`RETURN_CONSTANT`, `THROW_EXCEPTION`, `CALL_SUPER`, `RETURN_DEFAULT`). |
| `bytebuddy_transform_class` | Weaves ByteBuddy advice into class methods matching regex patterns. | `className` / `bytecodeBase64`, `rules` (`MEASURE_EXECUTION_TIME`, `LOG_CALL_AND_RETURN`, `OVERRIDE_RETURN_VALUE`, `SUPPRESS_EXCEPTIONS`). |
| `bytebuddy_disassemble` | Returns both **ASM opcode assembly** and pristine **decompiled Java source (CFR)**. | `className` or `bytecodeBase64`, `includeDecompiledJava`, `includeOpcodes`. |
| `bytebuddy_inspect` | Inspects class hierarchy, interfaces, method signatures, modifiers, and bytecode size. | `className` or `bytecodeBase64`. |
| `bytebuddy_execute_method` | Instantiates class in an isolated ClassLoader, executes target method, and captures stdout and return value. | `className`, `methodName`, `arguments`. |
| `bytebuddy_list_classes` | Lists all classes currently loaded or generated in the active MCP session. | — |
| `bytebuddy_load_bytes` | Ingests arbitrary raw Base64 `.class` bytecode for instant inspection, disassembly, or transformation. | `bytecodeBase64`, `className` (optional). |
| `bytebuddy_load_from_jar` | Inspects or imports classes directly from a local `.jar` archive (supports individual classes or batch import via `*`). | `jarPath`, `className` (optional). |

---

## 🚀 Quick Start & Building

### Prerequisites
* **Java 17+** (JDK 17, 21, or 25+ supported)
* Gradle (wrapper provided) or Maven

### Build Executable Fat JAR
```bash
# Using Gradle
./gradlew fatJar

# Built artifact:
# build/libs/bytebuddy-mcp-server-1.0.0-all.jar
```

### Run Tests
```bash
./gradlew test
```

---

## 🔌 Connecting to AI Clients (MCP Setup)

Add this MCP server configuration to your AI client's settings:

### Claude Desktop / Claude Code
File: `claude_desktop_config.json` or `.mcp.json`
```json
{
  "mcpServers": {
    "bytebuddy": {
      "command": "java",
      "args": [
        "-jar",
        "/Volumes/External/Code/java-asm-mcp/build/libs/bytebuddy-mcp-server-1.0.0-all.jar"
      ]
    }
  }
}
```

### Antigravity / Cursor / Cline
```json
{
  "mcpServers": {
    "bytebuddy-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/Volumes/External/Code/java-asm-mcp/build/libs/bytebuddy-mcp-server-1.0.0-all.jar"
      ]
    }
  }
}
```

> **Note on stdio compliance**: All application logs are strictly routed to `System.err`, ensuring that `System.out` is reserved exclusively for JSON-RPC 2.0 protocol frames.

---

## 💡 Example Prompt for AI Assistant

Once connected, your AI assistant can be prompted like:

> *"Generate a dynamic Java class `com.demo.PaymentCalculator` with a method `calculateTotal` returning a constant integer `100`. Then weave execution timing advice into it, disassemble it to verify the bytecode instructions, and execute the method."*

---

## 📄 License
MIT License.
