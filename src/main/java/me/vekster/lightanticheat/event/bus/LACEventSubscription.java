package me.vekster.lightanticheat.event.bus;

import java.util.Objects;
import java.util.function.Consumer;

final class LACEventSubscription {

    private final Object owner;
    private final String methodName;
    private final Consumer<Object> consumer;

    LACEventSubscription(Object owner, String methodName, Consumer<Object> consumer) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        this.owner = owner;
        this.methodName = methodName;
        this.consumer = consumer;
    }

    Object getOwner() {
        return owner;
    }

    String getMethodName() {
        return methodName;
    }

    Consumer<Object> getConsumer() {
        return consumer;
    }

    void accept(Object event) {
        consumer.accept(event);
    }

}
