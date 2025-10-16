package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MapObject implements HasFields, HasIndex {
    final Map<Object, Object> map;

    public MapObject(Map<Object, Object> map) {
        this.map = map;
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        map.put(name, value);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        return map.get(name);
    }

    @Override
    public Set<String> fields() {
        return map.keySet().stream().map(Object::toString).collect(Collectors.toSet());
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        map.put(index, value);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        return map.get(index);
    }

    @Override
    public Iterator<Object> iterator() {
        return map.keySet().iterator();
    }
}
