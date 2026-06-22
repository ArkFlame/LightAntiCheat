package me.vekster.lightanticheat.util.hook.server.folia;

import me.vekster.lightanticheat.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private static Method isOwnedByCurrentRegionLocationRadiusMethod;
    private static Method isOwnedByCurrentRegionWorldChunkRadiusMethod;
    private static Method isOwnedByCurrentRegionEntityMethod;
    private static Method isOwnedByCurrentRegionBlockMethod;

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
            isOwnedByCurrentRegionLocationRadiusMethod = Bukkit.getServer().getClass()
                    .getMethod("isOwnedByCurrentRegion", Location.class, int.class);
            isOwnedByCurrentRegionWorldChunkRadiusMethod = Bukkit.getServer().getClass()
                    .getMethod("isOwnedByCurrentRegion", World.class, int.class, int.class, int.class);
            isOwnedByCurrentRegionEntityMethod = Bukkit.getServer().getClass()
                    .getMethod("isOwnedByCurrentRegion", Entity.class);
            isOwnedByCurrentRegionBlockMethod = Bukkit.getServer().getClass()
                    .getMethod("isOwnedByCurrentRegion", Block.class);
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

    public static boolean isOwnedByCurrentRegion(final Location location, final int squareRadiusChunks) {
        if (!folia) return Bukkit.isPrimaryThread();
        if (location == null) return false;
        if (location.getWorld() == null) return false;
        return invokeOwnership(isOwnedByCurrentRegionLocationRadiusMethod, location, Math.max(0, squareRadiusChunks));
    }

    public static boolean isOwnedByCurrentRegion(final World world, final int chunkX, final int chunkZ, final int squareRadiusChunks) {
        if (!folia) return Bukkit.isPrimaryThread();
        if (world == null) return false;
        return invokeOwnership(isOwnedByCurrentRegionWorldChunkRadiusMethod, world, chunkX, chunkZ, Math.max(0, squareRadiusChunks));
    }

    public static boolean isOwnedByCurrentRegion(final Entity entity) {
        if (!folia) return Bukkit.isPrimaryThread();
        if (entity == null) return false;
        return invokeOwnership(isOwnedByCurrentRegionEntityMethod, entity);
    }

    public static boolean isOwnedByCurrentRegion(final Block block) {
        if (!folia) return Bukkit.isPrimaryThread();
        if (block == null) return false;
        return invokeOwnership(isOwnedByCurrentRegionBlockMethod, block);
    }

    private static boolean invokeOwnership(final Method method, final Object... args) {
        try {
            final Object result = method.invoke(Bukkit.getServer(), args);
            return result instanceof Boolean && (Boolean) result;
        } catch (final ReflectiveOperationException exception) {
            Main.getInstance().getLogger().warning("Failed to invoke Folia ownership check method " + method.getName() + ".");
            return false;
        }
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
        isOwnedByCurrentRegionLocationRadiusMethod = null;
        isOwnedByCurrentRegionWorldChunkRadiusMethod = null;
        isOwnedByCurrentRegionEntityMethod = null;
        isOwnedByCurrentRegionBlockMethod = null;
    }
}
