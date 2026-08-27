package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.input.model.LACMovementFrame;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class LACPlayerInputQueue implements AutoCloseable {

    public static final int MAX_FRAMES_PER_DRAIN = 128;

    private final LACPlayerSession session;
    private final ConcurrentLinkedQueue<QueuedItem> queue = new ConcurrentLinkedQueue<QueuedItem>();
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);
    private final Consumer<QueuedItem> dispatcher;
    private volatile boolean closed;

    public static final class QueuedItem {
        private final LACPacketFrame packetFrame;
        private final Optional<LACMovementFrame> movementFrame;

        public QueuedItem(LACPacketFrame packetFrame, Optional<LACMovementFrame> movementFrame) {
            if (packetFrame == null) {
                throw new IllegalArgumentException("packetFrame must not be null");
            }
            if (movementFrame == null) {
                throw new IllegalArgumentException("movementFrame must not be null");
            }
            this.packetFrame = packetFrame;
            this.movementFrame = movementFrame;
        }

        public LACPacketFrame getPacketFrame() {
            return packetFrame;
        }

        public Optional<LACMovementFrame> getMovementFrame() {
            return movementFrame;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof QueuedItem)) return false;
            QueuedItem other = (QueuedItem) object;
            return Objects.equals(packetFrame, other.packetFrame)
                    && Objects.equals(movementFrame, other.movementFrame);
        }

        @Override
        public int hashCode() {
            int result = packetFrame != null ? packetFrame.hashCode() : 0;
            result = 31 * result + (movementFrame != null ? movementFrame.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "QueuedItem{packetFrame=" + packetFrame + ", movementFrame=" + movementFrame + '}';
        }
    }

    public LACPlayerInputQueue(LACPlayerSession session, Consumer<QueuedItem> dispatcher) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        this.session = session;
        this.dispatcher = dispatcher;
    }

    public LACPlayerInputQueue(LACPlayerSession session) {
        this(session, null);
    }

    public LACPlayerSession getSession() {
        return session;
    }

    public boolean isClosed() {
        return closed;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void enqueue(LACPacketFrame packetFrame) {
        enqueue(packetFrame, Optional.<LACMovementFrame>empty());
    }

    public void enqueue(LACPacketFrame packetFrame, Optional<LACMovementFrame> movementFrame) {
        if (closed) {
            return;
        }
        if (packetFrame == null) {
            throw new IllegalArgumentException("packetFrame must not be null");
        }
        if (movementFrame == null) {
            throw new IllegalArgumentException("movementFrame must not be null");
        }
        if (!session.equals(packetFrame.getSession())) {
            return;
        }
        if (packetFrame.getInputEpoch() != session.getPlayerEpoch()) {
            return;
        }
        if (movementFrame.isPresent()) {
            LACMovementFrame mf = movementFrame.get();
            if (!session.equals(mf.getSession())) {
                return;
            }
            if (mf.getInputEpoch() != session.getPlayerEpoch()) {
                return;
            }
        }
        queue.add(new QueuedItem(packetFrame, movementFrame));
        scheduleDrain();
    }

    private void scheduleDrain() {
        if (closed) {
            return;
        }
        if (drainScheduled.compareAndSet(false, true)) {
            LACPlayerManager.execute(session, true, this::drain);
        }
    }

    private void drain(LACPlayer.Context context) {
        if (closed) {
            queue.clear();
            drainScheduled.set(false);
            return;
        }
        if (context == null || !context.isCurrent()) {
            drainScheduled.set(false);
            if (!queue.isEmpty() && drainScheduled.compareAndSet(false, true)) {
                LACPlayerManager.execute(session, true, this::drain);
            }
            return;
        }
        if (!session.equals(context.owner().captureSession().orElse(null))) {
            if (!context.owner().matchesSession(session)) {
                drainScheduled.set(false);
                return;
            }
        }
        int processed = 0;
        QueuedItem item;
        while (processed < MAX_FRAMES_PER_DRAIN && (item = queue.poll()) != null) {
            try {
                if (dispatcher != null) {
                    dispatcher.accept(item);
                }
            } catch (RuntimeException ignored) {
            }
            processed++;
        }
        if (queue.isEmpty()) {
            drainScheduled.set(false);
            if (!queue.isEmpty() && drainScheduled.compareAndSet(false, true)) {
                LACPlayerManager.execute(session, true, this::drain);
            }
        } else {
            LACPlayerManager.execute(session, true, this::drain);
        }
    }

    @Override
    public void close() {
        closed = true;
        queue.clear();
        drainScheduled.set(false);
    }
}
