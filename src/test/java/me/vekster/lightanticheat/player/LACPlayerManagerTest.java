package me.vekster.lightanticheat.player;

import me.vekster.lightanticheat.player.cache.PlayerCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LACPlayerManagerTest {

    static {
        installMockServer();
    }

    private static void installMockServer() {
        if (Bukkit.getServer() != null) return;
        Server server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class[]{Server.class},
                (proxy, method, args) -> null);
        try {
            Field field = Bukkit.class.getDeclaredField("server");
            field.setAccessible(true);
            field.set(null, server);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @BeforeEach
    void clearRegistry() throws Exception {
        Field field = LACPlayerManager.class.getDeclaredField("PLAYERS");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    // ---------------------------------------------------------------------
    // Proxied collaborators
    // ---------------------------------------------------------------------

    private static final class PlayerHolder {
        UUID uuid = UUID.randomUUID();
        World world;
        boolean online = true;
        double x = 0.0;
        double y = 64.0;
        double z = 0.0;
    }

    private static final class WorldHolder {
        final UUID uid = UUID.randomUUID();
    }

    private static World newWorldProxy(WorldHolder holder) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class[]{World.class},
                new WorldHandler(holder));
    }

    private static final class WorldHandler implements InvocationHandler {
        private final WorldHolder holder;

        WorldHandler(WorldHolder holder) {
            this.holder = holder;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getUID":
                    return holder.uid;
                case "getName":
                    return "world";
                case "getEnvironment":
                    return org.bukkit.World.Environment.NORMAL;
                case "toString":
                    return "world";
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static final class PlayerHandler implements InvocationHandler {
        private final PlayerHolder holder;

        PlayerHandler(PlayerHolder holder) {
            this.holder = holder;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "getUniqueId":
                    return holder.uuid;
                case "getWorld":
                    return holder.world;
                case "getLocation":
                    return new Location(holder.world, holder.x, holder.y, holder.z);
                case "isOnline":
                    return holder.online;
                case "getName":
                    return "test";
                case "toString":
                    return "test";
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == void.class) return null;
        return 0;
    }

    private Player newPlayer() {
        PlayerHolder holder = new PlayerHolder();
        holder.world = newWorldProxy(new WorldHolder());
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class[]{Player.class},
                new PlayerHandler(holder));
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    void attachCreatesCurrentContext() {
        Player player = newPlayer();
        LACPlayer wrapper = LACPlayerManager.attach(player);
        Optional<LACPlayer.Context> ctx = LACPlayerManager.current(player);
        assertTrue(ctx.isPresent());
        assertEquals(wrapper, ctx.get().owner());
        assertEquals(player, ctx.get().player());
        assertEquals(player.getWorld().getUID(), ctx.get().worldId());
    }

    @Test
    void rejoinReusesWrapper() {
        Player player = newPlayer();
        LACPlayer first = LACPlayerManager.attach(player);
        LACPlayer second = LACPlayerManager.attach(player);
        assertSame(first, second);
    }

    @Test
    void rejoinInvalidatesOldContext() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        LACPlayer.Context c1 = LACPlayerManager.capture(player).get();
        LACPlayerManager.attach(player);
        assertFalse(c1.isCurrent());
    }

    @Test
    void beginTransitionInvalidatesContext() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        LACPlayer.Context c = LACPlayerManager.capture(player).get();
        LACPlayerManager.beginTransition(player);
        assertFalse(c.isCurrent());
    }

    @Test
    void completeTransitionUsesActualWorld() {
        Player player = newPlayer();
        World worldA = player.getWorld();
        LACPlayerManager.attach(player);

        World worldB = newWorldProxy(new WorldHolder());
        PlayerHolder holder = findHolder(player);
        holder.world = worldB;

        LACPlayerManager.beginTransition(player);
        LACPlayerManager.completeTransition(player);

        LACPlayer.Context ctx = LACPlayerManager.capture(player).get();
        assertEquals(worldB.getUID(), ctx.worldId());
        assertNotEquals(worldA.getUID(), ctx.worldId());
    }

    @Test
    void detachInvalidatesContext() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        LACPlayer.Context c = LACPlayerManager.capture(player).get();
        LACPlayerManager.detach(player);
        assertFalse(c.isCurrent());
    }

    @Test
    void oldRefreshFinishDoesNotClearNewReservation() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        LACPlayer wrapper = LACPlayerManager.find(player.getUniqueId()).get();

        LACPlayer.Context c1 = LACPlayerManager.capture(player).get();
        assertTrue(wrapper.tryQueueStateRefresh(c1));

        LACPlayerManager.beginTransition(player);
        LACPlayerManager.completeTransition(player);
        LACPlayer.Context c2 = LACPlayerManager.capture(player).get();

        AtomicReference<LACPlayer.Context> queued = wrapper.queuedStateRefresh;
        queued.set(c2);
        wrapper.finishStateRefresh(c1);

        assertSame(c2, queued.get());
    }

    @Test
    void managerFindUsesSingleRegistry() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        assertTrue(LACPlayerManager.find(player.getUniqueId()).isPresent());
        assertTrue(LACPlayerManager.find(player).isPresent());

        UUID uuid = player.getUniqueId();
        LACPlayer first = LACPlayerManager.find(uuid).get();
        LACPlayerManager.attach(player);
        LACPlayer second = LACPlayerManager.find(uuid).get();
        assertSame(first, second);
        assertEquals(1, LACPlayerManager.values().size());
    }

    @Test
    void capturePacketSafeNoBukkitCalls() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        Optional<LACPlayer.Context> cap = LACPlayerManager.capture(player);
        assertTrue(cap.isPresent());
        assertTrue(cap.get().epoch() >= 1);
    }

    @Test
    void contextHasValueSemantics() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        LACPlayer.Context c1 = LACPlayerManager.capture(player).get();
        LACPlayer.Context c2 = LACPlayerManager.capture(player).get();

        // equality is value-based and both contexts are equal because the underlying
        // captured epoch/cache/world/player/owner all match.
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        // toString contains field names
        String str = c1.toString();
        assertTrue(str.contains("owner="));
        assertTrue(str.contains("epoch="));

        // equals against null and unrelated types returns false
        assertNotEquals(c1, null);
        assertNotEquals(c1, "not a context");
    }

    @Test
    void concurrentAttachCreatesSingleWrapper() throws Exception {
        Player player = newPlayer();
        final UUID playerUuid = player.getUniqueId();
        final int workers = 8;
        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(workers);
        final java.util.concurrent.ConcurrentLinkedQueue<LACPlayer> seen =
                new java.util.concurrent.ConcurrentLinkedQueue<>();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(workers);
        try {
            for (int i = 0; i < workers; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        LACPlayer wrapper = LACPlayerManager.attach(player);
                        seen.add(wrapper);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        LACPlayer registry = LACPlayerManager.find(playerUuid).orElseThrow(AssertionError::new);
        for (LACPlayer wrapper : seen) {
            assertSame(registry, wrapper);
        }
        assertEquals(1, LACPlayerManager.values().size());
    }

    @Test
    void valuesReturnsUnmodifiableSnapshot() {
        Player player = newPlayer();
        LACPlayerManager.attach(player);
        java.util.Collection<LACPlayer> snap = LACPlayerManager.values();
        assertEquals(1, snap.size());
        // iterating works
        int count = 0;
        for (LACPlayer ignored : snap) {
            count++;
        }
        assertEquals(1, count);
        // mutating the snapshot throws
        assertThrows(UnsupportedOperationException.class, () -> snap.clear());
        assertThrows(UnsupportedOperationException.class, () -> snap.remove(LACPlayerManager.find(player.getUniqueId()).orElseThrow(AssertionError::new)));
    }

    // ---------------------------------------------------------------------
    // Helper to reach the mutable holder behind a Player proxy
    // ---------------------------------------------------------------------

    private static PlayerHolder findHolder(Player player) {
        InvocationHandler handler = Proxy.getInvocationHandler(player);
        return ((PlayerHandler) handler).holder;
    }
}
