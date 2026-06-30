package me.vekster.lightanticheat.event.bus;

import me.vekster.lightanticheat.Main;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class LACEventBus {

    private static final Map<LACEventType, EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>>> BUS =
            new EnumMap<>(LACEventType.class);
    private static final LACEventPriority[] PRIORITIES = LACEventPriority.values();
    private static final Map<LACEventType, LACEventSubscription[]> SNAPSHOTS = new EnumMap<>(LACEventType.class);

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
        SNAPSHOTS.clear();
    }

    public static void call(LACEventType type, Object event) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(event, "event must not be null");

        final LACEventSubscription[] subscriptions;
        synchronized (LACEventBus.class) {
            subscriptions = SNAPSHOTS.get(type);
        }
        if (subscriptions == null || subscriptions.length == 0) return;

        for (LACEventSubscription subscription : subscriptions) {
            if (!subscription.shouldCall(event)) {
                continue;
            }
            try {
                subscription.accept(event);
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.SEVERE,
                        "Exception in event bus subscription: "
                                + subscription.getOwner().getClass().getName()
                                + "." + subscription.getMethodName(), e);
            }
        }
    }

    private static synchronized void rebuildAllSnapshots() {
        SNAPSHOTS.clear();
        for (LACEventType type : BUS.keySet()) {
            rebuildSnapshot(type);
        }
    }

    private static synchronized void rebuildSnapshot(final LACEventType type) {
        final EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>> priorityMap = BUS.get(type);
        if (priorityMap == null) {
            SNAPSHOTS.remove(type);
            return;
        }
        int size = 0;
        for (LACEventPriority priority : PRIORITIES) {
            final CopyOnWriteArrayList<LACEventSubscription> subscriptions = priorityMap.get(priority);
            if (subscriptions != null) {
                size += subscriptions.size();
            }
        }
        if (size == 0) {
            SNAPSHOTS.remove(type);
            return;
        }
        final LACEventSubscription[] snapshot = new LACEventSubscription[size];
        int index = 0;
        for (LACEventPriority priority : PRIORITIES) {
            final CopyOnWriteArrayList<LACEventSubscription> subscriptions = priorityMap.get(priority);
            if (subscriptions == null || subscriptions.isEmpty()) {
                continue;
            }
            for (LACEventSubscription subscription : subscriptions) {
                snapshot[index++] = subscription;
            }
        }
        SNAPSHOTS.put(type, snapshot);
    }

}
