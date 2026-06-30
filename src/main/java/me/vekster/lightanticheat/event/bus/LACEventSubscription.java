package me.vekster.lightanticheat.event.bus;

import java.util.Objects;
import java.util.function.Consumer;

final class LACEventSubscription {

    private final Object owner;
    private final String methodName;
    private final LACEventPriority priority;
    final LACMovementRequirement movementRequirement;
    private final Consumer<Object> consumer;

    LACEventSubscription(Object owner, String methodName, LACEventPriority priority,
                         LACMovementRequirement movementRequirement, Consumer<Object> consumer) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(movementRequirement, "movementRequirement must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        this.owner = owner;
        this.methodName = methodName;
        this.priority = priority;
        this.movementRequirement = movementRequirement;
        this.consumer = consumer;
    }

    Object getOwner() {
        return owner;
    }

    String getMethodName() {
        return methodName;
    }

    LACEventPriority getPriority() {
        return priority;
    }

    Consumer<Object> getConsumer() {
        return consumer;
    }

    boolean shouldCall(Object event) {
        return movementRequirement.accepts(event);
    }

    void accept(Object event) {
        consumer.accept(event);
    }

    boolean acceptsMask(final int movementMask) {
        return movementRequirement.acceptsMask(movementMask);
    }

    void call(final Object event) {
        consumer.accept(event);
    }

}
