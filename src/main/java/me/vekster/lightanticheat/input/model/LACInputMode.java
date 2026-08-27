package me.vekster.lightanticheat.input.model;

import java.util.Locale;
import java.util.Optional;

public enum LACInputMode {
    PACKET,
    NMS;

    public static Optional<LACInputMode> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if ("packet".equals(lower)) {
            return Optional.of(PACKET);
        }
        if ("nms".equals(lower)) {
            return Optional.of(NMS);
        }
        return Optional.empty();
    }
}
