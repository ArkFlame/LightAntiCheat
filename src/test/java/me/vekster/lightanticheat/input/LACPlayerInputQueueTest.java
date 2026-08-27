package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.input.model.LACLocation;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.scheduler.gamescheduler.GameScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LACPlayerInputQueueTest {

    static {
        installMockServer();
    }

    private static void installMockServer() {
        Server current = Bukkit.getServer();
        if (current != null) {
            // patch existing mock to answer isPrimaryThread=true if it doesn't already
            try {
                Method m = current.getClass().getMethod("isPrimaryThread");
                // probe: if proxy already returns true we keep it
                Object probe = Proxy.getInvocationHandler(current);
                // just reinstall correct mock
            } catch (Exception ignored) {}
        }
        Server server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class[]{Server.class},
                (proxy, method, args) -> {
                    if ("isPrimaryThread".equals(method.getName())) return true;
                    return defaultValue(method.getReturnType());
                });
        try {
            Field f = Bukkit.class.getDeclaredField("server");
            f.setAccessible(true);
            f.set(null, server);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setScheduler(GameScheduler value) {
        try {
            Field f = me.vekster.lightanticheat.util.scheduler.Scheduler.class.getDeclaredField("SCHEDULER");
            // use Unsafe to overwrite static final
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            Object unsafe = theUnsafeField.get(null);
            Method staticFieldBase = unsafeClass.getMethod("staticFieldBase", Field.class);
            Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
            Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
            Object base = staticFieldBase.invoke(unsafe, f);
            long offset = (long) staticFieldOffset.invoke(unsafe, f);
            putObject.invoke(unsafe, base, offset, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object defaultValue(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return false;
        if (t == char.class) return '\0';
        if (t == void.class) return null;
        return 0;
    }

    // ---- fake scheduler ----

    private static final class FakeScheduler implements GameScheduler {
        final ConcurrentLinkedQueue<Runnable> pending = new ConcurrentLinkedQueue<>();
        final AtomicInteger scheduleCount = new AtomicInteger(0);

        void reset() { pending.clear(); scheduleCount.set(0); }
        int drainAll() {
            int n = 0;
            Runnable r;
            while ((r = pending.poll()) != null) { r.run(); n++; }
            return n;
        }
        Runnable poll() { return pending.poll(); }

        @Override public void runTask(boolean ignoreOnFolia, Runnable task) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskAsynchronously(boolean ignoreOnFolia, Runnable task) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskLater(Runnable task, long delayInTicks) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskLater(Entity entity, Runnable task, long delayInTicks) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskLaterAsynchronously(Runnable task, long delayInTicks) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskTimer(Runnable task, long delayInTicks, long periodInTicks) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskTimer(Entity entity, Runnable task, long delayInTicks, long periodInTicks) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void runTaskTimerAsynchronously(Runnable task, long delayInTicks, long periodInTicks) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void entityThread(Player player, Runnable task) { pending.add(task); scheduleCount.incrementAndGet(); }
        @Override public void entityThread(Player player, boolean force, Runnable task) { pending.add(task); scheduleCount.incrementAndGet(); }
    }

    private FakeScheduler fake;
    private GameScheduler originalScheduler;

    @BeforeEach
    void setUp() throws Exception {
        clearPlayers();
        Field f = me.vekster.lightanticheat.util.scheduler.Scheduler.class.getDeclaredField("SCHEDULER");
        f.setAccessible(true);
        originalScheduler = (GameScheduler) f.get(null);
        fake = new FakeScheduler();
        setScheduler(fake);
    }

    @AfterEach
    void tearDown() throws Exception {
        clearPlayers();
        if (originalScheduler != null) {
            setScheduler(originalScheduler);
        }
    }

    @SuppressWarnings("unchecked")
    private void clearPlayers() throws Exception {
        Field f = LACPlayerManager.class.getDeclaredField("PLAYERS");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    // ---- helpers for player/world ----

    private static final class WorldHolder { final UUID uid = UUID.randomUUID(); }
    private static final class PlayerHolder { UUID uuid = UUID.randomUUID(); World world; boolean online = true; }

    private static World newWorld(WorldHolder h) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class[]{World.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUID": return h.uid;
                        case "getName": return "world";
                        case "getEnvironment": return World.Environment.NORMAL;
                        case "toString": return "world";
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static Player newPlayer(World world, UUID uuid) {
        PlayerHolder holder = new PlayerHolder();
        holder.world = world;
        if (uuid != null) holder.uuid = uuid;
        InvocationHandler h = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return holder.uuid;
                case "getWorld": return holder.world;
                case "getLocation": return new Location(holder.world, 0, 64, 0);
                case "isOnline": return holder.online;
                case "getName": return "test";
                case "toString": return "test";
                case "equals": return proxy == args[0];
                case "hashCode": return System.identityHashCode(proxy);
                default: return defaultValue(method.getReturnType());
            }
        };
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class}, h);
    }

    private LACPlayer attachPlayer() {
        World w = newWorld(new WorldHolder());
        Player p = newPlayer(w, null);
        return LACPlayerManager.attach(p);
    }

    private LACPacketFrame frame(LACPlayerSession s, long seq) {
        return new LACPacketFrame(s, s.getPlayerEpoch(), seq, LACPacketType.FLYING, 1, Optional.empty(), 0L);
    }

    private LACPacketFrame frameWithEpoch(LACPlayerSession s, long epoch, long seq) {
        return new LACPacketFrame(s, epoch, seq, LACPacketType.FLYING, 1, Optional.empty(), 0L);
    }

    // ---- tests ----

    @Test
    void fifoOrder() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        for (int i = 0; i < 5; i++) q.enqueue(frame(sess, i));

        // drain all pending
        Runnable r;
        while ((r = fake.poll()) != null) r.run();

        assertEquals(5, out.size());
        for (int i = 0; i < 5; i++) assertEquals(i, out.get(i).getPacketFrame().getSequence());
        assertTrue(q.isEmpty());
    }

    @Test
    void onlyOneDrainScheduledForBurstEnqueue() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        for (int i = 0; i < 10; i++) q.enqueue(frame(sess, i));

        assertEquals(1, fake.scheduleCount.get(), "burst should schedule exactly once");
        assertEquals(10, q.size());

        // now drain
        Runnable r;
        while ((r = fake.poll()) != null) r.run();
        assertEquals(10, out.size());
    }

    @Test
    void staleInputEpochDropped() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        LACPacketFrame stale = frameWithEpoch(sess, sess.getPlayerEpoch() + 99, 1);
        q.enqueue(stale);
        assertEquals(0, q.size());
        assertEquals(0, fake.scheduleCount.get());

        q.enqueue(frame(sess, 2));
        assertEquals(1, q.size());
        assertEquals(1, fake.scheduleCount.get());
    }

    @Test
    void stalePlayerSessionDropped() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        LACPlayerSession other = new LACPlayerSession(UUID.randomUUID(), sess.getWorldId(), 1L);
        LACPacketFrame f = new LACPacketFrame(other, other.getPlayerEpoch(), 0, LACPacketType.FLYING, 1, Optional.empty(), 0L);
        q.enqueue(f);
        assertEquals(0, q.size());
        assertEquals(0, fake.scheduleCount.get());

        // also wrong world
        LACPlayerSession wrongWorld = new LACPlayerSession(sess.getPlayerId(), UUID.randomUUID(), sess.getPlayerEpoch());
        LACPacketFrame f2 = new LACPacketFrame(wrongWorld, wrongWorld.getPlayerEpoch(), 0, LACPacketType.FLYING, 1, Optional.empty(), 0L);
        q.enqueue(f2);
        assertEquals(0, q.size());
    }

    @Test
    void stalePlayerSessionAtDrainDropped() {
        LACPlayer lp = attachPlayer();
        Player bukkitPlayer = lp.peekPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        for (int i = 0; i < 3; i++) q.enqueue(frame(sess, i));
        assertEquals(1, fake.scheduleCount.get());

        // invalidate session before drain runs
        LACPlayerManager.beginTransition(bukkitPlayer);

        Runnable r;
        while ((r = fake.poll()) != null) r.run();

        // drain should have detected stale session and not dispatched
        assertEquals(0, out.size());
        // queue retains items? drain returns early without polling when session mismatch (see LACPlayerInputQueue:155-157)
        // it clears drainScheduled but does not poll. So items remain but won't be dispatched after invalidation.
        // The spec says "stale player session dropped" — verify no dispatch happened.
    }

    @Test
    void max128PerDrainThenContinuationReschedules() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        int total = 200;
        for (int i = 0; i < total; i++) q.enqueue(frame(sess, i));
        assertEquals(1, fake.scheduleCount.get());

        // run first drain
        Runnable first = fake.poll();
        assertNotNull(first);
        first.run();
        assertEquals(128, out.size());
        assertEquals(total - 128, q.size());
        Runnable second = fake.poll();
        assertNotNull(second, "continuation should have been scheduled");
        second.run();
        assertEquals(total, out.size());
        assertTrue(q.isEmpty());
        // FIFO preserved across chunks
        for (int i = 0; i < total; i++) assertEquals(i, out.get(i).getPacketFrame().getSequence());
    }

    @Test
    void closeClearsQueue() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        for (int i = 0; i < 5; i++) q.enqueue(frame(sess, i));
        assertEquals(5, q.size());
        q.close();
        assertTrue(q.isClosed());
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
        // draining after close should not dispatch
        Runnable r;
        while ((r = fake.poll()) != null) r.run();
        assertEquals(0, out.size());
    }

    @Test
    void noTaskAcceptedAfterClose() {
        LACPlayer lp = attachPlayer();
        LACPlayerSession sess = lp.captureSession().orElseThrow(AssertionError::new);
        List<LACPlayerInputQueue.QueuedItem> out = new ArrayList<>();
        LACPlayerInputQueue q = new LACPlayerInputQueue(sess, out::add);

        q.enqueue(frame(sess, 0));
        int countBeforeClose = fake.scheduleCount.get();
        q.close();
        fake.pending.clear(); // ignore pending from before close if any

        q.enqueue(frame(sess, 1));
        q.enqueue(frame(sess, 2));
        assertTrue(q.isEmpty());
        assertEquals(0, fake.pending.size(), "no task should be scheduled after close");
        // also scheduleCount should not increase after close
        assertEquals(countBeforeClose, fake.scheduleCount.get());
    }
}
