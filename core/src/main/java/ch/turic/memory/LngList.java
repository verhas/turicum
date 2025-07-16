package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class LngList implements HasIndex, HasFields {
    public final ArrayList<Object> array = new ArrayList<>();
    public final AtomicBoolean pinned = new AtomicBoolean(false);
    private final HasFields fieldProvider;

    public LngList() {
        this(null);
    }

    public LngList(HasFields fieldProvider) {
        this.fieldProvider = fieldProvider;
    }

    public HasFields getFieldProvider() {
        return fieldProvider;
    }

    public boolean hasFieldProvider() {
        return fieldProvider != null;
    }

    public void add(Object value) {
        array.add(value);
    }

    public void addAll(Collection<?> values) {
        array.addAll(values);
    }

    public static LngList of(){
        return new LngList();
    }
    public static LngList of(Object[] lst){
        LngList result = new LngList();
        result.array.addAll(Arrays.asList(lst));
        return result;
    }
    public static LngList of(Collection<?> lst){
        LngList result = new LngList();
        result.array.addAll(lst);
        return result;
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        ExecutionException.when(pinned.get(), "Cannot change a pinned list.");
        if (Cast.isLong(index)) {
            final var indexValue = Cast.toLong(index).intValue();
            if (indexValue < 0) {
                ExecutionException.when(array.size() + indexValue < 0, "Indexing error, %d is too small, %s-%s < %s.", indexValue,
                        array.size(), -indexValue, 0);
                array.set(array.size() + indexValue, value);
                return;
            }
            array.ensureCapacity(indexValue + 1);
            while (indexValue >= array.size()) {
                array.add(null);
            }
            array.set(indexValue, value);
            return;
        }
        if (index instanceof Range range) {
            final var start = range.getStart(array.size());
            final var end = range.getEnd(array.size());
            if (end < start) {
                throw new ExecutionException("reverse range [%d .. %d] cannot be set.", start, end);
            }
            if (value instanceof Iterable<?> iterable) {
                final var result = new ArrayList<>();
                int i = 0;
                for (; i < start; i++) {
                    result.add(array.get(i));
                }
                for (final var obj : iterable) {
                    result.add(obj);
                }
                for (i = end; i < array.size(); i++) {
                    result.add(this.array.get(i));
                }
                array.clear();
                array.addAll(result);
            } else {
                throw new ExecutionException("You cannot insert '%s' into a list", value);
            }
            return;
        }
        throw new ExecutionException("You cannot use '%s' as index", index);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        if (Cast.isLong(index)) {
            final var indexValue = Cast.toLong(index).intValue();
            ExecutionException.when(indexValue < 0, "Indexing error, %s < 0", indexValue);
            if (indexValue >= array.size()) {
                return null;
            } else {
                return array.get(indexValue);
            }
        }
        if (index instanceof Range range) {
            final var start = range.getStart(array.size());
            final var end = range.getEnd(array.size());
            final var result = new LngList(fieldProvider);
            for (int i = start; i < end; i++) {
                result.array.add(this.array.get(i));
            }
            return result;
        }
        throw new ExecutionException("You cannot use '%s' as index", index);
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        ExecutionException.when(pinned.get(), "Cannot set a field a pinned list.");
        if (fieldProvider == null) {
            throw new ExecutionException("List is not tied.");
        }
        fieldProvider.setField(name, value);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        if (fieldProvider == null) {
            throw new ExecutionException("List is not tied.");
        }
        return fieldProvider.getField(name);
    }

    @Override
    public Set<String> fields() {
        return Set.of("length");
    }

    @Override
    public Iterator<Object> iterator() {
        return array.iterator();
    }

    @Override
    public String toString() {
        return "[" +
                array.stream()
                        .map(s -> s == null ? "none" : s.toString()).
                        collect(Collectors.joining(", ")) +
                "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var lngList = (LngList) o;
        if (array.size() != lngList.array.size()) {
            return false;
        }
        final var compared = new IdentityHashMap<>();
        compared.put(lngList,null);
        compared.put(this,null);
        for (int i = 0; i < array.size(); i++) {
            final var thisField = array.get(i);
            final var thatField = lngList.array.get(i);
            if (!compared.containsKey(thisField) && !compared.containsKey(thatField) && !Objects.equals(thisField, thatField)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return computeHashCode(new IdentityHashMap<>());
    }

    private int computeHashCode(Map<Object, Boolean> visited) {
        if (visited.containsKey(this)) {
            return 0; // avoid cycles
        }
        visited.put(this, true);
        int result = 1;
        for (var item : array) {
            if (visited.containsKey(item)) {
                result = 31 * result;
            } else {
                result = 31 * result + (item == null ? 0 :
                        (item instanceof LngList l ? l.computeHashCode(visited) : item.hashCode()));
            }
        }
        return result;
    }

}
