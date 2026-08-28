package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.input.model.LACMovementFrame;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.input.provider.LACInputProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class LACInputEngine implements AutoCloseable {

    public interface ProviderFactory {
        LACInputProvider createPacketProvider(LACInputEngine engine) throws Exception;
        LACInputProvider createNmsProvider(Main plugin, LACInputEngine engine) throws Exception;
    }

    private static final ProviderFactory DEFAULT_FACTORY = new ProviderFactory() {
        @Override
        public LACInputProvider createPacketProvider(LACInputEngine engine) throws Exception {
            Class<?> clazz = Class.forName("me.vekster.lightanticheat.input.provider.packetevents.PacketEventsInputProvider");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(LACInputEngine.class);
            Object inst = ctor.newInstance(engine);
            return (LACInputProvider) inst;
        }

        @Override
        public LACInputProvider createNmsProvider(Main plugin, LACInputEngine engine) throws Exception {
            return new me.vekster.lightanticheat.input.provider.nms.NmsInputProvider(plugin, engine);
        }
    };

    private final Main plugin;
    private final ProviderFactory providerFactory;
    private final AtomicLong inputEpoch = new AtomicLong(1L);
    private final AtomicReference<LACInputMode> activeMode = new AtomicReference<>();
    private final LACInputDispatcher dispatcher;
    private volatile LACInputProvider packetProvider;
    private volatile LACInputProvider nmsProvider;
    private volatile boolean closed;

    private final ConcurrentHashMap<UUID, LACPlayerInputQueue> queues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final LACBukkitStateBridge bridge;

    public LACInputEngine(final Main plugin, final LACInputMode initialMode) {
        this(plugin, initialMode, DEFAULT_FACTORY);
    }

    LACInputEngine(final Main plugin, final LACInputMode initialMode, final ProviderFactory providerFactory) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        if (initialMode == null) {
            throw new IllegalArgumentException("initialMode must not be null");
        }
        if (providerFactory == null) {
            throw new IllegalArgumentException("providerFactory must not be null");
        }
        this.plugin = plugin;
        this.providerFactory = providerFactory;
        this.dispatcher = new LACInputDispatcher(this);
        this.bridge = new LACBukkitStateBridge(this);
        activateInitialMode(initialMode);
    }

    private void activateInitialMode(final LACInputMode mode) {
        if (mode == LACInputMode.PACKET) {
            try {
                ensurePacketProvider();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start listener-mode '" + mode + "': " + e.getMessage(), e);
            } catch (LinkageError e) {
                throw new IllegalStateException("Failed to start listener-mode '" + mode + "': " + e.getMessage(), e);
            }
        } else if (mode == LACInputMode.NMS) {
            try {
                ensureNmsProvider();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start listener-mode '" + mode + "': " + e.getMessage(), e);
            } catch (LinkageError e) {
                throw new IllegalStateException("Failed to start listener-mode '" + mode + "': " + e.getMessage(), e);
            }
        }
        activeMode.set(mode);
    }

    private synchronized void ensurePacketProvider() throws Exception {
        if (packetProvider != null && packetProvider.isStarted()) {
            return;
        }
        if (packetProvider == null) {
            packetProvider = providerFactory.createPacketProvider(this);
        }
        packetProvider.start();
    }

    private synchronized void ensureNmsProvider() throws Exception {
        if (nmsProvider != null && nmsProvider.isStarted()) {
            return;
        }
        if (nmsProvider == null) {
            nmsProvider = providerFactory.createNmsProvider(plugin, this);
        }
        nmsProvider.start();
    }

    public synchronized void reconfigure(LACInputMode target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        if (closed) {
            throw new IllegalStateException("LAC input engine is closed");
        }
        LACInputMode current = activeMode.get();
        if (target == current) {
            return;
        }
        try {
            if (target == LACInputMode.PACKET) {
                ensurePacketProvider();
            } else {
                ensureNmsProvider();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start listener-mode '" + target + "': " + e.getMessage(), e);
        } catch (LinkageError e) {
            throw new IllegalStateException("Failed to start listener-mode '" + target + "': " + e.getMessage(), e);
        }
        LACInputMode previous = activeMode.get();
        inputEpoch.incrementAndGet();
        activeMode.set(target);
        if (previous == LACInputMode.NMS && target != LACInputMode.NMS) {
            LACInputProvider toClose = nmsProvider;
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (Exception ignored) {
                }
                nmsProvider = null;
            }
        }
    }

    public LACBukkitStateBridge getBridge() {
        return bridge;
    }

    public LACBukkitStateBridge getBukkitStateBridge() {
        return bridge;
    }

    public LACInputMode getActiveMode() {
        final LACInputMode mode = activeMode.get();
        if (mode == null) {
            throw new IllegalStateException("LAC input engine has no active provider.");
        }
        return mode;
    }

    public Optional<LACInputMode> getActiveModeOptional() {
        return Optional.ofNullable(activeMode.get());
    }

    public long getInputEpoch() {
        return inputEpoch.get();
    }

    public LACInputDispatcher getDispatcher() {
        return dispatcher;
    }

    public long nextSequence(LACPlayerSession session) {
        if (session == null) {
            return 0L;
        }
        AtomicLong counter = sequences.computeIfAbsent(session.getPlayerId(), k -> new AtomicLong(0L));
        return counter.incrementAndGet();
    }

    public void enqueue(LACPacketFrame frame, Optional<LACMovementFrame> movement) {
        if (closed) {
            return;
        }
        if (frame == null) {
            return;
        }
        // discard frames with old engine epoch if frame epoch is engine epoch (when providers use engine epoch)
        // For compatibility with session-epoch frames, only discard if frame epoch < inputEpoch and frame epoch != session epoch
        // Minimal spec: frames with old epoch discarded - check against inputEpoch if present
        // We enforce: if frame epoch != session epoch and frame epoch != inputEpoch, discard is handled by queue anyway.
        // Additionally, if engine epoch has advanced beyond frame epoch (when frame uses engine epoch), discard.
        if (frame.getInputEpoch() < inputEpoch.get() && frame.getInputEpoch() != frame.getSession().getPlayerEpoch()) {
            // possible old engine epoch frame
            return;
        }
        UUID playerId = frame.getSession().getPlayerId();
        // Need dispatcher wiring for queue drain
        LACPlayerInputQueue queue = queues.get(playerId);
        if (queue == null) {
            LACPlayerInputQueue created = new LACPlayerInputQueue(frame.getSession(), dispatcher::dispatch);
            LACPlayerInputQueue prev = queues.putIfAbsent(playerId, created);
            queue = prev != null ? prev : created;
        }
        if (!queue.getSession().equals(frame.getSession())) {
            LACPlayerInputQueue replacement = new LACPlayerInputQueue(frame.getSession(), dispatcher::dispatch);
            queues.put(playerId, replacement);
            queue = replacement;
        }
        queue.enqueue(frame, movement != null ? movement : Optional.<LACMovementFrame>empty());
    }

    public void enqueue(LACPacketFrame frame) {
        enqueue(frame, Optional.<LACMovementFrame>empty());
    }

    public Optional<LACPlayerInputQueue> getQueue(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(queues.get(playerId));
    }

    public void remove(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LACPlayerInputQueue q = queues.remove(playerId);
        if (q != null) {
            try { q.close(); } catch (Exception ignored) {}
        }
        sequences.remove(playerId);
    }

    public void clear() {
        for (LACPlayerInputQueue q : queues.values()) {
            try {
                q.close();
            } catch (Exception ignored) {
            }
        }
        queues.clear();
        sequences.clear();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        inputEpoch.incrementAndGet();
        if (nmsProvider != null) {
            try {
                nmsProvider.close();
            } catch (Exception ignored) {
            }
            nmsProvider = null;
        }
        if (packetProvider != null) {
            try {
                packetProvider.close();
            } catch (Exception ignored) {
            }
            packetProvider = null;
        }
        clear();
    }
}
