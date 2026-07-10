package ch.turic.embed;

import ch.turic.Program;
import ch.turic.utils.Marshaller;

/**
 * A compiled, immutable Turicum program.
 * <p>
 * A program holds no execution state: the same instance can be evaluated any number of times,
 * concurrently, in any {@link TuriSession} of any {@link TuriEngine}. Compile once with
 * {@link TuriEngine#compile(String)} and reuse.
 */
public final class TuriProgram {
    final Program program;

    TuriProgram(Program program) {
        this.program = program;
    }

    /**
     * Serializes the compiled program to the binary {@code .turc} format. The bytes can be
     * stored and later loaded with {@link TuriEngine#load(byte[])}, skipping compilation.
     *
     * @return the serialized program
     */
    public byte[] serialize() {
        return new Marshaller().serialize(program);
    }
}
