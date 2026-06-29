package com.kireiko.utils.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Set;

public interface ServiceRegistry {
    @NotNull
    Set<ServiceKey<?>> keySet();

    @NotNull
    Set<Map.Entry<ServiceKey<?>, Provider<?>>> entrySet();

    @NotNull
    default <T> T get(@NotNull final ServiceKey<T> key) {
        final T service = getOrNull(key);

        if (service == null) {
            throw new NullPointerException("Service not found: " + key);
        } else {
            return service;
        }
    }

    @NotNull
    default <T> T get(@NotNull final Class<T> type) {
        return get(ServiceKey.key(type));
    }

    @Nullable
    <T> T getOrNull(@NotNull ServiceKey<T> key);

    @Nullable
    default <T> T getOrNull(@NotNull final Class<T> type) {
        return getOrNull(ServiceKey.key(type));
    }

    @Nullable
    default <T> T put(@NotNull final Class<T> type, final Provider<T> service) {
        return put(ServiceKey.key(type), service);
    }

    @Nullable
    <T> T put(@NotNull ServiceKey<T> key, Provider<T> service);

    @Nullable
    default <T> T put(@NotNull final Class<T> type, final T service) {
        return put(ServiceKey.key(type), service);
    }

    @Nullable
    <T> T put(@NotNull ServiceKey<T> key, T service);

    @Nullable
    <T> T putIfAbsent(@NotNull ServiceKey<T> key, T service);

    @Nullable
    default <T> T putIfAbsent(@NotNull final Class<T> type, final T service) {
        return putIfAbsent(ServiceKey.key(type), service);
    }

    @Nullable
    <T> T putIfAbsent(@NotNull ServiceKey<T> key, Provider<T> service);

    @Nullable
    default <T> T putIfAbsent(@NotNull final Class<T> type, final Provider<T> service) {
        return putIfAbsent(ServiceKey.key(type), service);
    }
}