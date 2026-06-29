package com.kireiko.utils.registry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistryImpl implements ServiceRegistry {
    private final Map<ServiceKey<?>, Provider<?>> registry = new ConcurrentHashMap<>();

    public ServiceRegistryImpl() {
    }

    @NotNull
    public Set<ServiceKey<?>> keySet() {
        return registry.keySet();
    }

    @NotNull
    public Set<Map.Entry<ServiceKey<?>, Provider<?>>> entrySet() {
        return registry.entrySet();
    }

    @Nullable
    public <T> T getOrNull(@NotNull final ServiceKey<T> key) {
        final Provider<T> provider = (Provider<T>) registry.get(key);
        return provider == null ? null : provider.get();
    }

    @Nullable
    public <T> T put(@NotNull final ServiceKey<T> key, final T service) {
        return put(key, singleton(service));
    }

    @Nullable
    public <T> T put(@NotNull final ServiceKey<T> key, final Provider<T> service) {
        return (T) registry.put(key, service);
    }

    @Nullable
    public <T> T putIfAbsent(@NotNull final ServiceKey<T> type, final T service) {
        return putIfAbsent(type, singleton(service));
    }

    @Nullable
    public <T> T putIfAbsent(@NotNull final ServiceKey<T> key, final Provider<T> service) {
        return (T) registry.putIfAbsent(key, service);
    }

    private static <T> Provider<T> singleton(final T service) {
        return () -> service;
    }
}