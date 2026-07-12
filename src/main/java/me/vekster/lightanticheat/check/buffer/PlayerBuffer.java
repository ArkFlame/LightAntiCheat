package me.vekster.lightanticheat.check.buffer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class PlayerBuffer {

    PlayerBuffer(boolean async) {
        variables = async ? new ConcurrentHashMap<>() : new HashMap<>();
        updated = System.currentTimeMillis();
    }

    private final Map<String, PlayerVariable> variables;
    volatile long updated;

    public boolean containsKey(String key) {
        updated = System.currentTimeMillis();
        return variables.containsKey(key);
    }

    public PlayerVariable get(String key) {
        updated = System.currentTimeMillis();
        return variables.get(key);
    }

    public void put(String key, PlayerVariable value) {
        updated = System.currentTimeMillis();
        variables.put(key, value);
    }

    static class PlayerVariable {
        public PlayerVariable(Object object) {
            this.object = object;
        }

        final Object object;
    }

}
