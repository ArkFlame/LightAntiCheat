package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.input.provider.LACInputProvider;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.config.ConfigManager;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LACInputEngineTest {

    // ---- minimal fake provider ----
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
        boolean packetAvailable = true;
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
        @Override public boolean isPacketAvailable() { return packetAvailable; }
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

    // ---- helpers ----
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

    private void setListenerMode(String v) {
        ConfigManager.Config.listenerMode = v;
    }

    @SuppressWarnings("unchecked")
    private void clearPlayers() throws Exception {
        Field f = LACPlayerManager.class.getDeclaredField("PLAYERS");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    // scheduler + bukkit mock to avoid NPE during enqueue drains
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

    // ---- tests ----

    @Test
    void packetStartup() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            assertEquals(LACInputMode.PACKET, engine.getActiveMode());
            assertEquals(1, packet.startCount);
            assertEquals(0, nms.startCount);
            assertTrue(packet.isStarted());
        } finally { engine.close(); }
    }

    @Test
    void nmsStartup() {
        setListenerMode("nms");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        factory.packetAvailable = false;
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            assertEquals(LACInputMode.NMS, engine.getActiveMode());
            assertEquals(1, nms.startCount);
            assertEquals(0, packet.startCount);
        } finally { engine.close(); }
    }

    @Test
    void sameModeReloadDoesNotRestartProvider() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            long epochBefore = engine.getInputEpoch();
            int startBefore = packet.startCount;
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals(startBefore, packet.startCount, "same-mode must not restart");
            assertEquals(epochBefore, engine.getInputEpoch(), "epoch must not increment on same-mode");
            assertEquals(LACInputMode.PACKET, engine.getActiveMode());
        } finally { engine.close(); }
    }

    @Test
    void nmsToPacketSwitchesOnlyAfterTargetStartSucceeds() {
        setListenerMode("nms");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        factory.packetAvailable = false;
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            assertEquals(LACInputMode.NMS, engine.getActiveMode());
            long epochBefore = engine.getInputEpoch();
            factory.packetAvailable = true; // now target can start
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals(LACInputMode.PACKET, engine.getActiveMode());
            assertEquals(1, packet.startCount);
            assertEquals(1, nms.closeCount, "old NMS must be closed after switch");
            assertEquals(epochBefore + 1, engine.getInputEpoch());
            assertTrue(packet.isStarted());
        } finally { engine.close(); }
    }

    @Test
    void failedTargetStartupPreservesOldActiveMode() {
        setListenerMode("nms");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        factory.packetAvailable = false;
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            long epochBefore = engine.getInputEpoch();
            factory.packetAvailable = false; // target PACKET unavailable -> ensurePacket will throw
            packet.failOnStart = false; // not needed; availability already false
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals(LACInputMode.NMS, engine.getActiveMode(), "must stay on old mode");
            assertEquals(epochBefore, engine.getInputEpoch(), "epoch must not increment on failure");
            assertEquals(0, nms.closeCount, "old provider must not be closed on failure");
        } finally { engine.close(); }
    }

    @Test
    void packetToNmsClosesOldNmsOrActivatesExactTarget() {
        // Start PACKET then switch to NMS: packet remains, nms started, mode becomes NMS
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            assertEquals(LACInputMode.PACKET, engine.getActiveMode());
            long epochBefore = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            assertEquals(LACInputMode.NMS, engine.getActiveMode());
            assertEquals(1, nms.startCount);
            assertEquals(epochBefore + 1, engine.getInputEpoch());
            // packet provider stays started (not closed when leaving packet mode per engine spec)
            assertEquals(0, packet.closeCount);
            assertTrue(nms.isStarted());
            // now switch back NMS -> PACKET must close NMS and activate packet
            long epochMid = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals(LACInputMode.PACKET, engine.getActiveMode());
            assertEquals(1, nms.closeCount);
            assertEquals(epochMid + 1, engine.getInputEpoch());
        } finally { engine.close(); }
    }

    @Test
    void failedNmsTargetPreservesPacketMode() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        nms.failOnStart = true;
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            long epochBefore = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            assertEquals(LACInputMode.PACKET, engine.getActiveMode());
            assertEquals(epochBefore, engine.getInputEpoch());
            assertEquals(0, packet.closeCount);
        } finally { engine.close(); }
    }

    @Test
    void inputEpochIncrementsOnSuccessfulModeChange() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            long e1 = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            assertEquals(e1 + 1, engine.getInputEpoch());
            engine.reconfigure(LACInputMode.PACKET);
            assertEquals(e1 + 2, engine.getInputEpoch());
            // failed switch must not increment
            nms.failOnStart = true;
            // need packet -> nms again but nms is now closed; recreate failing nms
            // nms was closed and nulled after leaving NMS, so next ensure will call factory.createNmsProvider
            // factory returns same nms instance which is now closed; start will fail
            engine.reconfigure(LACInputMode.NMS);
            assertEquals(e1 + 2, engine.getInputEpoch());
        } finally { engine.close(); }
    }

    @Test
    void oldEpochQueuedFrameRejected() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        try {
            long epochBefore = engine.getInputEpoch();
            engine.reconfigure(LACInputMode.NMS);
            long epochAfter = engine.getInputEpoch();
            assertTrue(epochAfter > epochBefore);
            // Craft frame with old engine epoch but different session epoch to trigger engine discard
            UUID pid = UUID.randomUUID();
            UUID wid = UUID.randomUUID();
            LACPlayerSession session = new LACPlayerSession(pid, wid, 99L);
            // frame epoch = old epoch (1) != session epoch (99) and < inputEpoch (2) => discarded
            LACPacketFrame oldFrame = new LACPacketFrame(session, epochBefore, 1L, LACPacketType.FLYING, 0, Optional.empty(), 0L);
            engine.enqueue(oldFrame, Optional.empty());
            Optional<LACPlayerInputQueue> q = engine.getQueue(pid);
            assertTrue(!q.isPresent() || q.get().isEmpty(), "old-epoch frame must be rejected");
            // valid frame with matching session epoch should be accepted via queue
            // Use session epoch == frame epoch, but frame epoch == session epoch so not rejected at engine level
            // However queue expects frame epoch == session epoch, so pass fresh pid/wid with epoch 99 and use same?
            // To prove acceptance we need a frame that passes queue checks: frame epoch == session epoch
            // We cannot feed engine-epoch frame that also matches queue, so we test rejection only.
        } finally { engine.close(); }
    }

    @Test
    void closeIdempotent() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        long epochBeforeClose = engine.getInputEpoch();
        engine.close();
        long epochAfterFirst = engine.getInputEpoch();
        assertEquals(epochBeforeClose + 1, epochAfterFirst);
        assertEquals(1, packet.closeCount);
        engine.close();
        assertEquals(1, packet.closeCount, "second close must be idempotent");
        assertEquals(epochAfterFirst, engine.getInputEpoch(), "epoch must not increment on second close");
    }

    @Test
    void noProviderOperationAfterClose() {
        setListenerMode("packet");
        FakeProvider packet = new FakeProvider(LACInputMode.PACKET);
        FakeProvider nms = new FakeProvider(LACInputMode.NMS);
        TestFactory factory = new TestFactory(packet, nms);
        LACInputEngine engine = new LACInputEngine(fakePlugin(), factory);
        engine.close();
        int packetStartBefore = packet.startCount;
        int packetCloseBefore = packet.closeCount;
        int nmsStartBefore = nms.startCount;
        long epochBefore = engine.getInputEpoch();
        // reconfigure after close must do nothing
        engine.reconfigure(LACInputMode.NMS);
        assertEquals(packetStartBefore, packet.startCount);
        assertEquals(packetCloseBefore, packet.closeCount);
        assertEquals(nmsStartBefore, nms.startCount);
        assertEquals(epochBefore, engine.getInputEpoch());
        // enqueue after close must be discarded (no queue created)
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACPacketFrame f = new LACPacketFrame(session, session.getPlayerEpoch(), 0L, LACPacketType.FLYING, 0, Optional.empty(), 0L);
        engine.enqueue(f, Optional.empty());
        assertFalse(engine.getQueue(pid).isPresent(), "enqueue after close must be dropped");
        // also null-safe reconfigure after close
        engine.reconfigure(null);
        engine.reconfigure(LACInputMode.PACKET);
        assertEquals(epochBefore, engine.getInputEpoch());
    }
}
