package ch.turic.utils;

import java.util.ArrayList;
import java.util.function.Predicate;

public class JdkTypePredicate implements Predicate<String> {
    public static final JdkTypePredicate INSTANCE = new JdkTypePredicate();
    private static final String[] packages;

    static {
        final var list = new ArrayList<String>();

        // Look at the boot module layer (includes all core Java modules)
        for (Module module : ModuleLayer.boot().modules()) {
            for (final var pkg : module.getPackages()) {
                if (pkg.startsWith("java.")) {
                    list.add(pkg + ".");
                }
            }
        }

        packages = list.toArray(String[]::new);
    }

    @Override
    public boolean test(String typeName) {
        for (final var p : packages) {
            if (typeName.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
