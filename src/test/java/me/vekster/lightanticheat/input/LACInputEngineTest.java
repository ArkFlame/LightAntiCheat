package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.input.provider.LACInputProvider;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.scheduler.gamescheduler.GameScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LACInputEngineTest {

    static final class FakeProvider implements LACInputProvider {
        final LACInputMode mode;
        boolean started;
        int startCount;
        int closeCount;
        boolean failOnStart;
        boolean throwOnClose;

        FakeProvider(LACInputMode mode) { this.mode = mode; }

        @Override public LACInputMode getMode() { return mode; }
        @Override public void start() {
            if (failOnStart) throw new IllegalStateException("fail start " + mode);
            if (started) return;
            started = true;
            startCount++;
        }
        @Override public boolean isStarted() { return started && closeCount == 0; }
        @Override public void close() {
            if (closeCount > 0) return;
            closeCount++;
            started = false;
            if (throwOnClose) throw new RuntimeException("close fail");
        }
    }

    static final class TestFactory implements LACInputEngine.ProviderFactory {
        FakeProvider packet;
        FakeProvider nms;
        int packetCreateCount;
        int nmsCreateCount;
        boolean packetFailCreate;
        boolean nmsFailCreate;

        TestFactory(FakeProvider packet, FakeProvider nms) {
            this.packet = packet;
            this.nms = nms;
        }
        @Override public LACInputProvider createPacketProvider(LACInputEngine engine) throws Exception {
            if (packetFailCreate) throw new IllegalStateException("create packet fail");
            packetCreateCount++;
            if (packet == null) packet = new FakeProvider(LACInputMode.PACKET);
            return packet;
        }
        @Override public LACInputProvider createNmsProvider(Main plugin, LACInputEngine engine) throws Exception {
            if (nmsFailCreate) throw new IllegalStateException("create nms fail");
            nmsCreateCount++;
            if (nms == null) nms = new FakeProvider(LACInputMode.NMS);
            return nms;
        }
    }

    private Main fakePlugin() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
            return (Main) allocate.invoke(unsafe, Main.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void clearPlayers() throws Exception {
        Field f = LACPlayerManager.class.getDeclaredField("PLAYERS");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    private GameScheduler originalScheduler;

    @BeforeEach
    void setUp() throws Exception {
        clearPlayers();
        installMockServer();
        Field f = me.vekster.lightanticheat.util.scheduler.Scheduler.class.getDeclaredField("SCHEDULER");
        f.setAccessible(true);
        originalScheduler = (GameScheduler) f.get(null);
        setScheduler(new FakeScheduler());
    }

    @AfterEach
    void tearDown() throws Exception {
        clearPlayers();
        if (originalScheduler != null) setScheduler(originalScheduler);
    }

    private static void installMockServer() {
        try {
            Server server = (Server) Proxy.newProxyInstance(
                    Server.class.getClassLoader(),
                    new Class[]{Server.class},
                    (proxy, method, args) -> {
                        if ("isPrimaryThread".equals(method.getName())) return true;
                        Class<?> r = method.getReturnType();
                        if (!r.isPrimitive()) return null;
                        if (r == boolean.class) return false;
                        if (r == void.class) return null;
                        return 0;
                    });
            Field sf = Bukkit.class.getDeclaredField("server");
            sf.setAccessible(true);
            sf.set(null, server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setScheduler(GameScheduler value) {
        try {
            Field f = me.vekster.lightanticheat.util.scheduler.Scheduler.class.getDeclaredField("SCHEDULER");
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class FakeScheduler implements GameScheduler {
        @Override public void runTask(boolean ignoreOnFolia, Runnable task) { if (task != null) task.run(); }
        @Override public void runTaskAsynchronously(boolean ignoreOnFolia, Runnable task) { if (task != null) task.run(); }
        @Override public void runTaskLater(Runnable task, long delayInTicks) { if (task != null) task.run(); }
        @Override public void runTaskLater(Entity entity, Runnable task, long delayInTicks) { if (task != null) task.run(); }
        @Override public void runTaskLaterAsynchronously(Runnable task, long delayInTicks) { if (task != null) task.run(); }
        @Override public void runTaskTimer(Runnable task, long delayInTicks, long periodInTicks) { if (task != null) task.run(); }
        @Override public void runTaskTimer(Entity entity, Runnable task, long delayInTicks, long periodInTicks) { if (task != null) task.run(); }
        @Override public void runTaskTimerAsynchronously(Runnable task, long delayInTicks, long periodInTicks) { if (task != null) task.run(); }
        @Override public void entityThread(Player player, Runnable task) { if (task != null) task.run(); }
        @Override public void entityThread(Player player, boolean force, Runnable task) { if (task != null) task.run(); }
    }

    // ---- 14 required tests ----

    @Test
    void packetStartupStartsPacketOnly() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            assertEquals("1/0/PACKET/1", packet.startCount + "/" + nms.startCount + "/" + engine.getActiveMode() + "/" + engine.getInputEpoch());
        } finally { engine.close(); }
    }

    @Test
    void nmsStartupStartsNmsOnly() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.NMS, factory);
        try {
            assertEquals("0/1/NMS", packet.startCount + "/" + nms.startCount + "/" + engine.getActiveModeOptional().orElse(null));
        } finally { engine.close(); }
    }

    @Test
    void packetStartupFailureDoesNotFallbackToNms() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        packet.failOnStart = true;
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        assertThrows(IllegalStateException.class, () -> new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory));
        assertEquals("0/0", nms.startCount + "/" + factory.nmsCreateCount);
    }

    @Test
    void nmsStartupFailureDoesNotFallbackToPacket() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        nms.failOnStart = true;
        TestFactory factory = new TestFactory(packet, nms);
        assertThrows(IllegalStateException.class, () -> new LACInputEngine(fakePlugin(), LACInputMode.NMS, factory));
        assertEquals("0/0", packet.startCount + "/" + factory.packetCreateCount);
    }

    @Test
    void sameModeReconfigureDoesNotRestartOrAdvanceEpoch() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            long epochBefore = engine.getInputEpoch();
            int startBefore = packet.startCount;
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals(startBefore + "/" + epochBefore + "/PACKET", packet.startCount + "/" + engine.getInputEpoch() + "/" + engine.getActiveMode());
        } finally { engine.close(); }
    }

    @Test
    void failedPacketReconfigureThrowsAndPreservesNms() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.NMS, factory);
        try {
            long epochBefore = engine.getInputEpoch();
            packet.failOnStart = true;
            assertThrows(IllegalStateException.class, () -> engine.reconfigure(LACInputMode.PACKET));
            assertEquals("NMS/" + epochBefore + "/0/0", engine.getActiveMode() + "/" + engine.getInputEpoch() + "/" + nms.closeCount + "/" + packet.startCount);
        } finally { engine.close(); }
    }

    @Test
    void failedNmsReconfigureThrowsAndPreservesPacket() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        nms.failOnStart = true;
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            long epochBefore = engine.getInputEpoch();
            assertThrows(IllegalStateException.class, () -> engine.reconfigure(LACInputMode.NMS));
            assertEquals("PACKET/" + epochBefore + "/0", engine.getActiveMode() + "/" + engine.getInputEpoch() + "/" + packet.closeCount);
        } finally { engine.close(); }
    }

    @Test
    void successfulNmsToPacketPublishesThenClosesNms() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.NMS, factory);
        try {
            long epochBefore = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals("PACKET/1/1/" + (epochBefore + 1), engine.getActiveMode() + "/" + packet.startCount + "/" + nms.closeCount + "/" + engine.getInputEpoch());
        } finally { engine.close(); }
    }

    @Test
    void successfulPacketToNmsStartsNmsAndKeepsPacketListenerDormant() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            long epochBefore = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            assertEquals("NMS/1/0/" + (epochBefore + 1), engine.getActiveMode() + "/" + nms.startCount + "/" + packet.closeCount + "/" + engine.getInputEpoch());
        } finally { engine.close(); }
    }

    @Test
    void successfulModeChangeAdvancesEpochExactlyOnce() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            long e0 = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            assertEquals("" + (e0 + 1), "" + engine.getInputEpoch());
        } finally { engine.close(); }
    }

    @Test
    void closedEngineRejectsReconfigure() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        engine.close();
        assertThrows(IllegalStateException.class, () -> engine.reconfigure(LACInputMode.NMS));
    }

    @Test
    void nullTargetRejectsReconfigure() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            assertThrows(IllegalArgumentException.class, () -> engine.reconfigure(null));
        } finally { engine.close(); }
    }

    @Test
    void closeIsIdempotent() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        long epochBeforeClose = engine.getInputEpoch();
        engine.close();
        long epochAfterFirst = engine.getInputEpoch();
        engine.close();
        assertEquals("1/" + (epochBeforeClose + 1) + "/" + epochAfterFirst, packet.closeCount + "/" + epochAfterFirst + "/" + engine.getInputEpoch());
    }

    @Test
    void oldEpochFrameStillRejected() {
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), LACInputMode.PACKET, factory);
        try {
            long epochBefore = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            long epochAfter = engine.getInputEpoch();
            assertTrue(epochAfter > epochBefore);
            UUID pid = UUID.randomUUID();
            UUID wid = UUID.randomUUID();
            LACPlayerSession session = new LACPlayerSession(pid, wid, 99L);
            LACPacketFrame oldFrame = new LACPacketFrame(session, epochBefore, 1L, LACPacketType.FLYING, 0, Optional.empty(), 0L);
            engine.enqueue(oldFrame, Optional.empty());
            Optional<LACPlayerInputQueue> q = engine.getQueue(pid);
            assertEquals("true", String.valueOf(!q.isPresent() || q.get().isEmpty()));
        } finally { engine.close(); }
    }
}
