package ch.turic.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * A dynamic class loader that can add new jars to the classpath.
 */
public class TuricumClassLoader extends ClassLoader {

    private final AtomicReference<URLClassLoader> delegate = new AtomicReference<>();

    public TuricumClassLoader(ClassLoader parent) {
        super(parent);
        delegate.set(new URLClassLoader(new URL[0], parent));
    }

    public void inherit(TuricumClassLoader followed) {
        for (final var url : followed.delegate.get().getURLs()) {
            try {
                addJar(Path.of(url.toURI()));
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void addJar(Path jarPath) throws MalformedURLException {
        URL[] oldUrls = getUrls();
        URL newUrl = jarPath.toUri().toURL();
        URL[] updated = Arrays.copyOf(oldUrls, oldUrls.length + 1);
        updated[oldUrls.length] = newUrl;
        final var old = delegate.getAndSet(new URLClassLoader(updated, getParent()));
        try {
            old.close();
        } catch (Exception ignore) {
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = getPathToClass(name);

        URL[] urls = getUrls();
        for (URL url : urls) {
            try (final var is = getByteCodeInputStream(url, path)) {
                byte[] bytes = is.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (URISyntaxException | IOException ignored) {
            }
        }

        throw new ClassNotFoundException(name);
    }

    /**
     * Creates an input stream for reading byte code from a given URL and path.
     * The method supports URLs with "jar", "file", or other protocols, providing access to the byte code
     * either directly or by opening the corresponding file or JAR entry.
     *
     * @param url  the URL from which the byte code should be retrieved, supporting "jar", "file", or other protocols
     * @param path the path to the specific resource within the URL, used for JAR or file systems
     * @return an InputStream for reading the byte code from the specified resource
     * @throws IOException        if an I/O error occurs while opening the stream or accessing the resource
     * @throws URISyntaxException if the URL syntax is invalid and cannot be converted to a URI
     */
    private static InputStream getByteCodeInputStream(URL url, String path) throws IOException, URISyntaxException {
        String protocol = url.getProtocol();

        if ("jar".equals(protocol)) {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            return conn.getInputStream();
        } else if ("file".equals(protocol) && url.getPath().endsWith(".jar")) {
            final var uri = URI.create("jar:" + url.toURI() + "!/");
            final var fs = getFileSystem(uri);
            final var entry = fs.getPath(path);
            final var is = Files.newInputStream(entry);
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return is.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return is.read(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    try {
                        is.close();
                    } finally {
                        fs.close();
                    }
                }
            };
        } else {
            // Normal file or HTTP URL
            return url.toURI().resolve(path).toURL().openStream();
        }
    }

    private static FileSystem getFileSystem(URI uri) throws IOException {
        FileSystem fs;
        try {
            fs = FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            fs = FileSystems.newFileSystem(uri, Map.of());
        }
        return fs;
    }

    /**
     * Retrieves the URLs associated with the underlying delegate's class loader.
     *
     * @return an array of URLs representing the classpath entries of the underlying delegate
     */
    private URL[] getUrls() {
        return delegate.get().getURLs();
    }

    /**
     * Converts a given fully qualified class name to its corresponding file path,
     * using '/' as the separator and appending the ".class" extension.
     *
     * @param name the fully qualified name of the class (e.g., "com.example.MyClass")
     * @return the file path to the class in the format "com/example/MyClass.class"
     */
    private static String getPathToClass(String name) {
        return name.replace('.', '/') + ".class";
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve); // parent-first
        } catch (ClassNotFoundException ignore) {
        }
        URL[] urls = getUrls();
        String path = getPathToClass(name);

        for (URL url : urls) {
            try (final var is = getByteCodeInputStream(url, path)) {
                byte[] bytes = is.readAllBytes();
                Class<?> cls = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(cls);
                return cls;
            } catch (URISyntaxException | IOException ignored) {
            }
        }
        throw new ClassNotFoundException(name);

    }

    @Override
    public String getName() {
        return "TuricumClassLoader";
    }

    @Override
    public URL getResource(String name) {
        final var url = super.getResource(name);
        if (url == null) {
            return delegate.get().getResource(name);
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return delegate.get().getResources(name);
    }

    @Override
    public Stream<URL> resources(String name) {
        return delegate.get().resources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        final var stream = super.getResourceAsStream(name);
        if (stream == null) {
            return delegate.get().getResourceAsStream(name);
        }
        return stream;
    }


    private static final class CompoundEnumeration<E> implements Enumeration<E> {
        private final Enumeration<E>[] enums;
        private int index;

        @SafeVarargs
        private CompoundEnumeration(Enumeration<E>... enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (index < enums.length) {
                if (enums[index] != null && enums[index].hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        public boolean hasMoreElements() {
            return next();
        }

        public E nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return enums[index].nextElement();
        }
    }

}