package com.antigravity.bytebuddy.mcp.infrastructure.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A bounded ByteArrayOutputStream that prevents OutOfMemoryError by capping output capture.
 */
public class BoundedByteArrayOutputStream extends OutputStream {
    private static final int DEFAULT_MAX_BYTES = 2 * 1024 * 1024; // 2 MB safety cap
    private static final byte[] TRUNCATION_NOTICE = "\n...[Output truncated: exceeded 2 MB safety limit]...".getBytes(StandardCharsets.UTF_8);

    private final ByteArrayOutputStream buffer;
    private final int maxBytes;
    private boolean truncated = false;

    public BoundedByteArrayOutputStream() {
        this(DEFAULT_MAX_BYTES);
    }

    public BoundedByteArrayOutputStream(int maxBytes) {
        this.buffer = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        this.maxBytes = maxBytes;
    }

    @Override
    public synchronized void write(int b) {
        if (buffer.size() < maxBytes) {
            buffer.write(b);
        } else if (!truncated) {
            try {
                buffer.write(TRUNCATION_NOTICE);
            } catch (IOException ignored) {}
            truncated = true;
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        if (buffer.size() + len <= maxBytes) {
            buffer.write(b, off, len);
        } else if (!truncated) {
            int available = Math.max(0, maxBytes - buffer.size());
            if (available > 0) {
                buffer.write(b, off, available);
            }
            try {
                buffer.write(TRUNCATION_NOTICE);
            } catch (IOException ignored) {}
            truncated = true;
        }
    }

    public synchronized String toString(java.nio.charset.Charset charset) {
        return buffer.toString(charset);
    }

    public synchronized int size() {
        return buffer.size();
    }
}
