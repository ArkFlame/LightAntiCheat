package me.vekster.lightanticheat.util.hook.server.folia;

import me.vekster.lightanticheat.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaUtil {
    private static boolean folia;
    private static Method getGlobalRegionSchedulerMethod;
    private static Method getAsyncSchedulerMethod;
    private static Method getRegionSchedulerMethod;
    private static Method entityGetSchedulerMethod;
    private static Method entityTeleportAsyncMethod;

    private FoliaUtil() {
    }

    public static void loadFoliaUtil() {
        folia = classExists("io.papermc.paper.threadedregions.RegionizedServer");
        if (!folia) {
            clearMethods();
            return;
        }
        try {
            getGlobalRegionSchedulerMethod = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            getAsyncSchedulerMethod = Bukkit.getServer().getClass().getMethod("getAsyncScheduler");
            getRegionSchedulerMethod = Bukkit.getServer().getClass().getMethod("getRegionScheduler");
            entityGetSchedulerMethod = Entity.class.getMethod("getScheduler");
            entityTeleportAsyncMethod = Entity.class.getMethod("teleportAsync", Location.class);
        } catch (final ReflectiveOperationException exception) {
            folia = false;
            clearMethods();
            Main.getInstance().getLogger().warning("Folia detected but scheduler API is unavailable. Falling back to Bukkit scheduler bridge.");
        }
    }

    public static boolean isFolia() {
        return folia;
    }

    public static boolean isStable(final Player player) {
        return player != null && player.isOnline();
    }

    public static void runTask(final Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        invokeGlobal("run", new Class<?>[]{Plugin.class, Consumer.class}, plugin(), (Consumer<Object>) ignored -> runnable.run());
    }

    public static void runTask(final Entity entity, final Runnable runnable) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        invokeEntity(entity, "execute", new Class<?>[]{Plugin.class, Runnable.class, Runnable.class, long.class}, plugin(), runnable, null, 1L);
    }

    public static void runTaskAsynchronously(final Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        invokeAsync("runNow", new Class<?>[]{Plugin.class, Consumer.class}, plugin(), (Consumer<Object>) ignored -> runnable.run());
    }

    public static void runTaskLater(final Runnable runnable, final long delay) {
        Objects.requireNonNull(runnable, "runnable");
        invokeGlobal("runDelayed", new Class<?>[]{Plugin.class, Consumer.class, long.class}, plugin(), (Consumer<Object>) ignored -> runnable.run(), Math.max(1L, delay));
    }

    public static void runTaskLater(final Entity entity, final Runnable runnable, final long delay) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(runnable, "runnable");
        invokeEntity(entity, "execute", new Class<?>[]{Plugin.class, Runnable.class, Runnable.class, long.class}, plugin(), runnable, null, Math.max(1L, delay));
    }

    public static void runTaskLaterAsynchronously(final Runnable runnable, final long delay) {
        Objects.requireNonNull(runnable, "runnable");
        invokeAsync("runDelayed", new Class<?>[]{Plugin.class, Consumer.class, long.class, TimeUnit.class}, plugin(), (Consumer<Object>) ignored -> runnable.run(), ticksToMillis(delay), TimeUnit.MILLISECONDS);
    }

    public static void runTaskTimer(final Runnable task, final long delay, final long period) {
        Objects.requireNonNull(task, "task");
        invokeGlobal("runAtFixedRate", new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class}, plugin(), (Consumer<Object>) ignored -> task.run(), Math.max(1L, delay), Math.max(1L, period));
    }

    public static void runTaskTimer(final Entity entity, final Runnable task, final long delay, final long period) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(task, "task");
        invokeEntity(entity, "runAtFixedRate", new Class<?>[]{Plugin.class, Consumer.class, Runnable.class, long.class, long.class}, plugin(), (Consumer<Object>) ignored -> task.run(), null, Math.max(1L, delay), Math.max(1L, period));
    }

    public static void runTaskTimerAsynchronously(final Runnable task, final long delay, final long period) {
        Objects.requireNonNull(task, "task");
        invokeAsync("runAtFixedRate", new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class}, plugin(), (Consumer<Object>) ignored -> task.run(), ticksToMillis(delay), ticksToMillis(period), TimeUnit.MILLISECONDS);
    }

    public static void teleportPlayer(final Player player, final Location location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        if (!folia) {
            player.teleport(location);
            return;
        }
        try {
            entityTeleportAsyncMethod.invoke(player, location);
        } catch (final ReflectiveOperationException exception) {
            Main.getInstance().getLogger().warning("Failed to invoke Folia teleportAsync for " + player.getName() + ".");
        }
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleportPlayerAsync(final Player player, final Location location) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        if (!folia) {
            return CompletableFuture.completedFuture(player.teleport(location));
        }
        try {
            final Object result = entityTeleportAsyncMethod.invoke(player, location);
            if (result instanceof CompletableFuture) {
                return (CompletableFuture<Boolean>) result;
            }
        } catch (final ReflectiveOperationException exception) {
            Main.getInstance().getLogger().warning("Failed to invoke Folia teleportAsync for " + player.getName() + ".");
        }
        return CompletableFuture.completedFuture(false);
    }

    private static Plugin plugin() {
        return Main.getInstance();
    }

    private static long ticksToMillis(final long ticks) {
        return Math.max(1L, ticks) * 50L;
    }

    private static void invokeGlobal(final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        try {
            final Object scheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
            scheduler.getClass().getMethod(methodName, parameterTypes).invoke(scheduler, args);
        } catch (final ReflectiveOperationException exception) {
            Main.getInstance().getLogger().warning("Failed to invoke Folia global scheduler method " + methodName + ".");
        }
    }

    private static void invokeAsync(final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        try {
            final Object scheduler = getAsyncSchedulerMethod.invoke(Bukkit.getServer());
            scheduler.getClass().getMethod(methodName, parameterTypes).invoke(scheduler, args);
        } catch (final ReflectiveOperationException exception) {
            Main.getInstance().getLogger().warning("Failed to invoke Folia async scheduler method " + methodName + ".");
        }
    }

    private static void invokeEntity(final Entity entity, final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        try {
            final Object scheduler = entityGetSchedulerMethod.invoke(entity);
            scheduler.getClass().getMethod(methodName, parameterTypes).invoke(scheduler, args);
        } catch (final ReflectiveOperationException exception) {
            Main.getInstance().getLogger().warning("Failed to invoke Folia entity scheduler method " + methodName + " for entity " + entity.getUniqueId() + ".");
        }
    }

    private static boolean classExists(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    private static void clearMethods() {
        getGlobalRegionSchedulerMethod = null;
        getAsyncSchedulerMethod = null;
        getRegionSchedulerMethod = null;
        entityGetSchedulerMethod = null;
        entityTeleportAsyncMethod = null;
    }
}
