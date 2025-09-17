package ch.turic.memory.debugger;

import java.util.Objects;

public final class BreakPoint {
    final int line;

    public BreakPoint(int line) {
        this.line = line;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BreakPoint that = (BreakPoint) o;
        return line == that.line;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(line);
    }
}
