package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;

import java.util.ArrayList;
import java.util.List;

/**
 * The AllParents class implements the TuriFunction interface and provides
 * functionality to compute the transitive closure of parent classes for a
 * given class within the Turi language system. This computation collects all
 * direct and indirect parent classes of a specified class.
 * <p>
 * This class is registered under the name "all_parents" and is callable within
 * the Turi environment.
 *
 * <h2>Responsibilities:</h2>
 * - Retrieves and computes the full set of parent classes for a provided class.
 * - Utilizes the {@code parentClosure} method to recursively traverse up the
 * class hierarchy and collect parent classes.
 * - Implements the {@code call} method to integrate with the Turi execution
 * context and arguments.
 *
 * <h2>Thread Safety:</h2>
 * This class protects against {@code ConcurrentModificationException}
 * by creating immutable snapshots of parent collections during traversal using
 * {@code List.of()}. However, it does not guarantee that the entire class
 * hierarchy traversal represents a single consistent point-in-time view.
 * <p>
 * If the class hierarchy is modified during traversal by other threads, the
 * result may reflect parent relationships from different points in time, though
 * each individual parent relationship will have been valid at some point during
 * the traversal. This provides a good balance between consistency and performance
 * for most practical use cases.
 * <p>
 * The algorithm prevents infinite loops through duplicate detection using an
 * {@code ArrayList} that tracks already-visited classes during the recursive
 * traversal.
 * <p>
 * Example use:
 *
 * <pre>{@code
 * class A {}
 * class B : A {}
 * let K = class : B {}
 * die "all parents is wrong" when all_parents(K) != [B, A]
 * }</pre>
 */
public class AllParents implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var lngClass = FunUtils.arg(name(), arguments, LngClass.class);
        final var parentList = new ArrayList<LngClass>();
        parentClosure(parentList, lngClass);
        final var parents = new LngList();
        parents.array.addAll(parentList);
        return parents;
    }

    private static void parentClosure(List<LngClass> parentList, LngClass lngClass) {
        for (final var p : ((ClassContext) lngClass.context()).parents()) {
            if (!parentList.contains(p)) {
                parentList.add(p);
                parentClosure(parentList, p);
            }
        }
    }


}
