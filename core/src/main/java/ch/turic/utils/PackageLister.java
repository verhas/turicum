package ch.turic.utils;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;


/**
 * The PackageLister class provides functionality to list all classes from a specified package.
 * It uses a provided ClassLoader to search for classes in the specified package.
 * The classes can be loaded from either the file system or a JAR file.
 */
public record PackageLister(ClassLoader classLoader) {

    public Set<Class<?>> listClasses(final String packageName) throws Exception {
        final var classes = new HashSet<Class<?>>();
        final var packagePath = packageName.replace('.', '/');
        final var packageUrls = classLoader.getResources(packagePath);
        while (packageUrls.hasMoreElements()) {
            final var packageUrl = packageUrls.nextElement();
            if (packageUrl == null) {
                return classes;
            }

            if (packageUrl.getProtocol().equals("file")) {
                final var packageDir = new File(URLDecoder.decode(packageUrl.getPath(), StandardCharsets.UTF_8));
                listClassesFromDirectory(packageDir, packageName, classes);
            } else if (packageUrl.getProtocol().equals("jar")) {
                final var jarPath = packageUrl.getPath().substring(5, packageUrl.getPath().indexOf("!"));
                final var jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8));
                listClassesFromJar(jar, packagePath, packageName, classes);
                jar.close();
            }
        }
        return classes;
    }

    private void listClassesFromDirectory(final File dir, final String packageName, final Set<Class<?>> classes) {
        if (!dir.exists()) return;

        final var files = dir.listFiles();
        if (files == null) return;

        for (final var file : files) {
            if (file.isDirectory()) {
                listClassesFromDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                final var className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className, false, classLoader));
                } catch (ClassNotFoundException e) {
                    // Skip classes that can't be loaded
                }
            }
        }
    }

    private void listClassesFromJar(final JarFile jar, final String packagePath, final String packageName, final Set<Class<?>> classes) {
        final var entries = jar.entries();
        while (entries.hasMoreElements()) {
            final var entry = entries.nextElement();
            final var name = entry.getName();

            if (name.startsWith(packagePath + "/") && name.endsWith(".class")) {
                final var className = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    classes.add(Class.forName(className, false, classLoader));
                } catch (ClassNotFoundException e) {
                    // Skip classes that can't be loaded
                }
            }
        }
    }

}