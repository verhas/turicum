package ch.turic.memory;

/**
 * A {@link Variable} whose value is stored in a {@code volatile} field.
 * <p>
 * Global variables are the one place where two interpreter threads can share mutable state
 * without passing through a synchronization point: a child thread may write a global while
 * the parent polls it, and no channel, lock or future connects the two accesses. Storing
 * global values in a volatile field guarantees that such a write becomes visible to the
 * other threads promptly.
 * <p>
 * Only the global heap ({@link GlobalContext#heap}) uses this class; see
 * {@link VarTable#VarTable(boolean)}. Local variables stay plain {@link Variable}s on purpose:
 * they are thread-confined, and whenever a local object crosses a thread boundary (channel
 * send/receive, async task start, future completion), the handoff mechanism itself already
 * provides the necessary happens-before edge. Keeping local variable access free of volatile
 * semantics keeps the interpreter's hottest path cheap.
 * <p>
 * Note that volatile storage guarantees <em>visibility</em>, not atomicity: concurrent
 * read-modify-write operations on a global (e.g. {@code x = x + 1} from two threads) can
 * still lose updates.
 */
public class VolatileVariable extends Variable {
    private volatile Object value;

    public VolatileVariable(String name) {
        super(name);
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    protected void assign(Object newValue) {
        this.value = newValue;
    }
}
