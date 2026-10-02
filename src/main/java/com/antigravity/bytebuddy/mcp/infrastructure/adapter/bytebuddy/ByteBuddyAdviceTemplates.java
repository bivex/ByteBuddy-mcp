package com.antigravity.bytebuddy.mcp.infrastructure.adapter.bytebuddy;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;

import java.util.Arrays;

/**
 * ByteBuddy advice templates for method instrumentation.
 * Inlined directly into target methods.
 */
public final class ByteBuddyAdviceTemplates {

    private ByteBuddyAdviceTemplates() {}

    /**
     * Measures and logs method execution duration.
     */
    public static class ExecutionTimeAdvice {
        @Advice.OnMethodEnter
        public static long onEnter() {
            return System.nanoTime();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(@Advice.Enter long startNano,
                                  @Advice.Origin("#t.#m(#s)") String signature,
                                  @Advice.Thrown Throwable thrown) {
            long durationMs = (System.nanoTime() - startNano) / 1_000_000;
            if (thrown != null) {
                System.err.printf("[ByteBuddy:ExecutionTime] %s failed with %s after %d ms%n",
                        signature, thrown.getClass().getSimpleName(), durationMs);
            } else {
                System.err.printf("[ByteBuddy:ExecutionTime] %s executed in %d ms%n", signature, durationMs);
            }
        }
    }

    /**
     * Logs method entry with arguments and exit with return value or exception.
     */
    public static class LoggingAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Origin("#t.#m") String method,
                                   @Advice.AllArguments Object[] args) {
            System.err.printf("[ByteBuddy:Call] Entering %s with args: %s%n",
                    method, (args != null ? Arrays.toString(args) : "[]"));
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(@Advice.Origin("#t.#m") String method,
                                  @Advice.Return(typing = Typing.DYNAMIC) Object returned,
                                  @Advice.Thrown Throwable thrown) {
            if (thrown != null) {
                System.err.printf("[ByteBuddy:Call] %s threw exception: %s: %s%n",
                        method, thrown.getClass().getName(), thrown.getMessage());
            } else {
                System.err.printf("[ByteBuddy:Call] %s returned: %s%n", method, returned);
            }
        }
    }

    /**
     * Suppresses any thrown exception and logs the suppression.
     */
    public static class ExceptionSuppressionAdvice {
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(@Advice.Origin("#t.#m") String method,
                                  @Advice.Thrown(readOnly = false) Throwable thrown) {
            if (thrown != null) {
                System.err.printf("[ByteBuddy:Suppressed] Suppressing exception in %s: %s%n",
                        method, thrown.getMessage());
                thrown = null;
            }
        }
    }
}
