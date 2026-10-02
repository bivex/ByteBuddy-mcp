package com.antigravity.bytebuddy.mcp.application.dto;

import java.time.Instant;

public record ClassSummaryResponse(
        String className,
        int bytecodeSize,
        int majorVersion,
        int methodCount,
        int fieldCount,
        Instant updatedAt
) {
}
