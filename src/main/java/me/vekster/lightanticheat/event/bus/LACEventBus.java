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
        subscriptions.add(new LACEventSubscription(owner, methodName, movementRequirement, consumer));
    }

    public static synchronized void unregister(Object owner) {
        if (owner == null) return;
        for (EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>> priorityMap : BUS.values()) {
            for (CopyOnWriteArrayList<LACEventSubscription> subscriptions : priorityMap.values()) {
                subscriptions.removeIf(sub -> owner.equals(sub.getOwner()));
            }
        }
    }

    public static synchronized void unregisterAll() {
        BUS.clear();
    }

    public static void call(LACEventType type, Object event) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(event, "event must not be null");

        EnumMap<LACEventPriority, CopyOnWriteArrayList<LACEventSubscription>> priorityMap;
        synchronized (BUS) {
            priorityMap = BUS.get(type);
        }
        if (priorityMap == null) return;

        for (LACEventPriority priority : LACEventPriority.values()) {
            CopyOnWriteArrayList<LACEventSubscription> subscriptions = priorityMap.get(priority);
            if (subscriptions == null || subscriptions.isEmpty()) continue;

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
    }

}
