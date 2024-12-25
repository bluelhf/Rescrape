package blue.lhf.rescrape;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class SizedInputStream extends InputStream {
    private final InputStream inner;
    private final long size;

    public SizedInputStream(final InputStream inner, final long size) {
        this.inner = inner;
        this.size = size;
    }


    @Override
    public int read() throws IOException {
        return inner.read();
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return inner.read(b, off, len);
    }

    public long getSize() {
        return size;
    }
}
