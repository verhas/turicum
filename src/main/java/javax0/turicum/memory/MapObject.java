package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;

import java.util.Map;

public class MapObject implements HasFields {
    final Map<Object, Object> map;

    public MapObject(Map<Object, Object> map) {
        this.map = map;
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        map.put(name,value);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        return map.get(name);
    }
}
