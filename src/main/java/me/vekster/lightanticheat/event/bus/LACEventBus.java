package me.vekster.lightanticheat.event.bus;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACMovementChange;
import me.vekster.lightanticheat.event.playermove.LACPlayerMoveEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class LACEventBus {

    private static final Map<LACEventType, EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>>> BUS =
            new EnumMap<>(LACEventType.class);
    private static final LACEventType[] TYPES = LACEventType.values();
    private static final LACEventPriority[] PRIORITIES = LACEventPriority.values();
    private static volatile LACEventSubscription[][] SNAPSHOTS = new LACEventSubscription[TYPES.length][];
    private static volatile LACEventSubscription[][][] MOVEMENT_SNAPSHOTS = new LACEventSubscription[TYPES.length][4][];
    private static final LACEventSubscription[] EMPTY = new LACEventSubscription[0];

    private LACEventBus() {
    }

    public static void register(LACEventType type, LACEventPriority priority,
                                Object owner, String methodName, Consumer<Object> consumer) {
        register(type, priority, owner, methodName, LACMovementRequirement.ANY, consumer);
    }

    public static synchronized void register(LACEventType type, LACEventPriority priority,
                                              Object owner, String methodName,
                                              LACMovementRequirement movementRequirement,
                                              Consumer<Object> consumer) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(movementRequirement, "movementRequirement must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        BUS.computeIfAbsent(type, k -> new EnumMap<>(LACEventPriority.class));
        EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>> priorityMap = BUS.get(type);
        priorityMap.computeIfAbsent(priority, k -> new CopyOnWriteArrayList<LACEventSubscription>());
        CopyOnWriteArrayList<LACEventSubscription> subscriptions = priorityMap.get(priority);
        subscriptions.add(new LACEventSubscription(owner, methodName, priority, movementRequirement, consumer));
        rebuildSnapshot(type);
    }

    public static synchronized void unregister(Object owner) {
        if (owner == null) return;
        for (EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>> priorityMap : BUS.values()) {
            for (CopyOnWriteArrayList<LACEventSubscription> subscriptions : priorityMap.values()) {
                subscriptions.removeIf(sub -> owner.equals(sub.getOwner()));
            }
        }
        rebuildAllSnapshots();
    }

    public static synchronized void unregisterAll() {
        BUS.clear();
        final int typeCount = TYPES.length;
        SNAPSHOTS = new LACEventSubscription[typeCount][];
        MOVEMENT_SNAPSHOTS = new LACEventSubscription[typeCount][4][];
    }

    private static int movementMask(final Object event) {
        if (event instanceof LACAsyncPlayerMoveEvent) {
            return movementMask(((LACAsyncPlayerMoveEvent) event).getMovementChange());
        }
        if (event instanceof LACPlayerMoveEvent) {
            return movementMask(((LACPlayerMoveEvent) event).getMovementChange());
        }
        return -1;
    }

    private static int movementMask(final LACMovementChange change) {
        if (change == null) {
            return -1;
        }
        int mask = 0;
        if (change.isPositionChanged()) {
            mask |= 1;
        }
        if (change.isRotationChanged()) {
            mask |= 2;
        }
        return mask;
    }

    public static void call(LACEventType type, Object event) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(event, "event must not be null");

        final int movementMask = movementMask(event);
        final LACEventSubscription[] subscriptions;
        if (movementMask >= 0) {
            final LACEventSubscription[][] byMask = MOVEMENT_SNAPSHOTS[type.ordinal()];
            subscriptions = byMask == null ? EMPTY : byMask[movementMask];
        } else {
            subscriptions = SNAPSHOTS[type.ordinal()];
        }
        if (subscriptions == null || subscriptions.length == 0) return;

        for (LACEventSubscription subscription : subscriptions) {
            try {
                subscription.call(event);
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.SEVERE,
                        "Exception in event bus subscription: "
                                + subscription.getOwner().getClass().getName()
                                + "." + subscription.getMethodName(), e);
            }
        }
    }

    private static synchronized void rebuildAllSnapshots() {
        final int typeCount = TYPES.length;
        final LACEventSubscription[][] newSnapshots = new LACEventSubscription[typeCount][];
        final LACEventSubscription[][][] newMovementSnapshots = new LACEventSubscription[typeCount][4][];
        for (int i = 0; i < typeCount; i++) {
            final LACEventType type = TYPES[i];
            newSnapshots[i] = buildSnapshot(type, -1);
            for (int mask = 0; mask < 4; mask++) {
                newMovementSnapshots[i][mask] = buildSnapshot(type, mask);
            }
        }
        SNAPSHOTS = newSnapshots;
        MOVEMENT_SNAPSHOTS = newMovementSnapshots;
    }

    private static synchronized void rebuildSnapshot(final LACEventType type) {
        final int index = type.ordinal();
        final LACEventSubscription[] all = buildSnapshot(type, -1);
        final LACEventSubscription[][] movement = new LACEventSubscription[4][];
        for (int mask = 0; mask < movement.length; mask++) {
            movement[mask] = buildSnapshot(type, mask);
        }
        final LACEventSubscription[][] snapshotsCopy = SNAPSHOTS.clone();
        snapshotsCopy[index] = all;
        SNAPSHOTS = snapshotsCopy;
        final LACEventSubscription[][][] movementCopy = MOVEMENT_SNAPSHOTS.clone();
        movementCopy[index] = movement;
        MOVEMENT_SNAPSHOTS = movementCopy;
    }

    private static LACEventSubscription[] buildSnapshot(final LACEventType type, final int mask) {
        final EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>> priorityMap = BUS.get(type);
        if (priorityMap == null) {
            return EMPTY;
        }
        int size = 0;
        for (final LACEventPriority priority : PRIORITIES) {
            final CopyOnWriteArrayList<LACEventSubscription> subscriptions = priorityMap.get(priority);
            if (subscriptions != null) {
                if (mask < 0) {
                    size += subscriptions.size();
                } else {
                    for (final LACEventSubscription sub : subscriptions) {
                        if (sub.acceptsMask(mask)) {
                            size++;
                        }
                    }
                }
            }
        }
        if (size == 0) {
            return EMPTY;
        }
        final LACEventSubscription[] snapshot = new LACEventSubscription[size];
        int index = 0;
        for (final LACEventPriority priority : PRIORITIES) {
            final CopyOnWriteArrayList<LACEventSubscription> subscriptions = priorityMap.get(priority);
            if (subscriptions == null || subscriptions.isEmpty()) {
                continue;
            }
            for (final LACEventSubscription subscription : subscriptions) {
                if (mask < 0 || subscription.acceptsMask(mask)) {
                    snapshot[index++] = subscription;
                }
            }
        }
        return snapshot;
    }

}
