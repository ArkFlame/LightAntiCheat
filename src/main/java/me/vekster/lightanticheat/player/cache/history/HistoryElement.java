package me.vekster.lightanticheat.player.cache.history;

public enum HistoryElement {
    FROM(0),
    FIRST(1),
    SECOND(2),
    THIRD(3),
    FOURTH(4),
    FIFTH(5),
    SIXTH(6),
    SEVENTH(7),
    EIGHT(8),
    NINTH(9),
    TENTH(10);

    private static final HistoryElement[] VALUES = values();

    private final int offset;

    HistoryElement(int offset) {
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }

    public static int count() {
        return VALUES.length;
    }

    public static HistoryElement at(int index) {
        if (index < 0 || index >= VALUES.length) {
            throw new IndexOutOfBoundsException("HistoryElement index: " + index);
        }
        return VALUES[index];
    }
}
