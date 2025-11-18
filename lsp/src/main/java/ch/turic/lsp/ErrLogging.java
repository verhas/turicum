package ch.turic.lsp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The ErrLogging class provides utility subclasses for logging standard input and output streams.
 * It defines two nested classes, InputStream and PrintStream, that extend their respective Java I/O classes.
 * These classes are designed to intercept data read or written to streams and log this data to
 * the standard error stream.
 */
public class ErrLogging {
    private static final int N = 102400;

    public static class InputStream extends java.io.InputStream {
        private final java.io.InputStream in;

        public InputStream(java.io.InputStream in) {
            this.in = in;
        }

        final byte[] buffer = new byte[N];
        int index = 0;

        private void fillBuffer(byte c) {
            buffer[index++] = c;
            if (index >= buffer.length) {
                System.err.print(new String(buffer, StandardCharsets.UTF_8));
                index = 0;
            }
        }

        @Override
        public int read() throws IOException {
            int c = in.read();
            fillBuffer((byte) c);
            return c;
        }

        @Override
        public void close() throws IOException {
            System.err.print(new String(buffer, 0, index, StandardCharsets.UTF_8));
            in.close();
        }
    }

    public static class PrintStream extends java.io.PrintStream {
        private final java.io.PrintStream out;

        public PrintStream(java.io.PrintStream out) {
            super(out, true);
            this.out = out;
        }

        final byte[] buffer = new byte[N];
        int index = 0;

        private synchronized void fillBuffer(byte c) {
            buffer[index++] = c;
            if (index >= buffer.length) {
                System.err.print(new String(buffer, StandardCharsets.UTF_8));
                index = 0;
            }
        }

        @Override
        public void write(int b) {
            fillBuffer((byte) b);
            out.write(b);
        }

        @Override
        public void close() {
            System.err.print(new String(buffer, 0, index, StandardCharsets.UTF_8));
            out.close();
        }
    }
}
