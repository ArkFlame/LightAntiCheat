package me.vekster.lightanticheat.player.violation;

import me.vekster.lightanticheat.check.CheckName;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

public class PlayerViolations {

    public enum NotificationChannel {
        VIOLATION_LOG,
        PUNISHMENT_LOG,
        VIOLATION_ALERT,
        PUNISHMENT_ALERT,
        VIOLATION_DISCORD,
        PUNISHMENT_DISCORD
    }

    private final AtomicIntegerArray violations = new AtomicIntegerArray(CheckName.values().length);
    private final AtomicLongArray notificationTimes = new AtomicLongArray(NotificationChannel.values().length);

    public int getViolations(CheckName checkName) {
        Objects.requireNonNull(checkName, "checkName");
        return violations.get(checkName.ordinal());
    }

    public void setViolations(CheckName checkName, int value) {
        Objects.requireNonNull(checkName, "checkName");
        violations.set(checkName.ordinal(), value);
    }

    public void increaseViolations(CheckName checkName, int value) {
        Objects.requireNonNull(checkName, "checkName");
        violations.addAndGet(checkName.ordinal(), value);
    }

    public boolean tryAcquire(NotificationChannel channel, long currentTimeMillis, long cooldownMillis) {
        Objects.requireNonNull(channel, "channel");
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("cooldownMillis must be >= 0");
        }
        while (true) {
            long previous = notificationTimes.get(channel.ordinal());
            long elapsed = currentTimeMillis - previous;
            if (elapsed <= cooldownMillis) {
                return false;
            }
            if (notificationTimes.compareAndSet(channel.ordinal(), previous, currentTimeMillis)) {
                return true;
            }
        }
    }

}
