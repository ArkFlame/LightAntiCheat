package me.vekster.lightanticheat.player.cache.history;

import java.util.Objects;

public class PlayerCacheHistory<T> {

    private final Object[] values = new Object[HistoryElement.count()];
    private int nextIndex;

    public PlayerCacheHistory(T initial) {
        Objects.requireNonNull(initial, "initial");
        for (int i = 0; i < values.length; i++) {
            values[i] = initial;
        }
        this.nextIndex = 0;
    }

    public synchronized void add(T value) {
        values[nextIndex] = value;
        nextIndex = (nextIndex + 1) % values.length;
    }

    @SuppressWarnings("unchecked")
    public synchronized T get(HistoryElement element) {
        int newest = Math.floorMod(nextIndex - 1, values.length);
        int idx = Math.floorMod(newest - element.offset(), values.length);
        return (T) values[idx];
    }

}
