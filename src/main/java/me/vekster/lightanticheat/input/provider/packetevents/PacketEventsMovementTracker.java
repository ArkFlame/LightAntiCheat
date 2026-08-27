package me.vekster.lightanticheat.input.provider.packetevents;

import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.vekster.lightanticheat.input.model.LACLocation;
import me.vekster.lightanticheat.input.model.LACMovementFrame;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PacketEventsMovementTracker {

    private static final class State {
        long playerEpoch;
        UUID worldId;
        LACLocation last;
        final AtomicLong packetSequence = new AtomicLong(0L);
    }

    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    public void clear() {
        states.clear();
    }

    public void remove(UUID playerId) {
        if (playerId == null) {
            return;
        }
        states.remove(playerId);
    }

    private boolean isFinite(double v) {
        return Double.isFinite(v);
    }

    private boolean isFinite(float v) {
        return Float.isFinite(v);
    }

    private LACLocation seedFor(LACPlayerSession session) {
        if (session == null) {
            return null;
        }
        Optional<LACPlayer> opt = LACPlayerManager.find(session.getPlayerId());
        if (opt.isPresent()) {
            LACLocation seed = opt.get().getSeedLocation();
            if (seed != null && seed.getWorldId() != null) {
                return seed;
            }
        }
        return null;
    }

    public Optional<LACMovementFrame> trackOrUpdate(LACPlayerSession session,
                                                    WrapperPlayClientPlayerFlying flying,
                                                    LACLocation seed) {
        if (session == null || flying == null) {
            return Optional.empty();
        }

        UUID playerId = session.getPlayerId();
        UUID worldId = session.getWorldId();
        long epoch = session.getPlayerEpoch();

        State state = states.get(playerId);
        if (state == null) {
            State created = new State();
            created.playerEpoch = epoch;
            created.worldId = worldId;
            LACLocation effectiveSeed = seed;
            if (effectiveSeed == null) {
                effectiveSeed = seedFor(session);
            }
            if (effectiveSeed == null) {
                Location loc = flying.getLocation();
                if (loc == null) {
                    return Optional.empty();
                }
                effectiveSeed = new LACLocation(worldId, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
            if (!isFinite(effectiveSeed.getX()) || !isFinite(effectiveSeed.getY()) || !isFinite(effectiveSeed.getZ())
                    || !isFinite(effectiveSeed.getYaw()) || !isFinite(effectiveSeed.getPitch())) {
                return Optional.empty();
            }
            created.last = effectiveSeed;
            State prev = states.putIfAbsent(playerId, created);
            state = prev != null ? prev : created;
            if (prev != null) {
                // already inserted by another thread, fall through to normal epoch/world check
            } else {
                // successfully seeded; now produce frame from seed
            }
        }

        synchronized (state) {
            if (state.playerEpoch != epoch || !state.worldId.equals(worldId)) {
                LACLocation effectiveSeed = seed;
                if (effectiveSeed == null) {
                    effectiveSeed = seedFor(session);
                }
                if (effectiveSeed == null) {
                    Location loc = flying.getLocation();
                    if (loc == null) {
                        states.remove(playerId);
                        return Optional.empty();
                    }
                    effectiveSeed = new LACLocation(worldId, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                }
                if (!isFinite(effectiveSeed.getX()) || !isFinite(effectiveSeed.getY()) || !isFinite(effectiveSeed.getZ())
                        || !isFinite(effectiveSeed.getYaw()) || !isFinite(effectiveSeed.getPitch())) {
                    states.remove(playerId);
                    return Optional.empty();
                }
                state.playerEpoch = epoch;
                state.worldId = worldId;
                state.last = effectiveSeed;
            }

            LACLocation from = state.last;
            if (from == null) {
                return Optional.empty();
            }

            Location raw = flying.getLocation();
            if (raw == null) {
                return Optional.empty();
            }

            boolean posChanged = flying.hasPositionChanged();
            boolean rotChanged = flying.hasRotationChanged();
            boolean onGround = flying.isOnGround();

            double x = posChanged ? raw.getX() : from.getX();
            double y = posChanged ? raw.getY() : from.getY();
            double z = posChanged ? raw.getZ() : from.getZ();
            float yaw = rotChanged ? raw.getYaw() : from.getYaw();
            float pitch = rotChanged ? raw.getPitch() : from.getPitch();

            if (!isFinite(x) || !isFinite(y) || !isFinite(z) || !isFinite(yaw) || !isFinite(pitch)) {
                return Optional.empty();
            }

            LACLocation to = new LACLocation(worldId, x, y, z, yaw, pitch);
            long seq = state.packetSequence.incrementAndGet();
            long now = System.currentTimeMillis();
            LACMovementFrame frame = new LACMovementFrame(
                    session, epoch, seq, from, to, posChanged, rotChanged, onGround, now
            );
            state.last = to;
            return Optional.of(frame);
        }
    }

    public Optional<LACMovementFrame> trackOrUpdate(LACPlayerSession session,
                                                    WrapperPlayClientPlayerFlying flying) {
        return trackOrUpdate(session, flying, null);
    }

    public Optional<LACMovementFrame> trackOrUpdate(LACPlayerSession session,
                                                    long inputEpoch,
                                                    long sequence,
                                                    WrapperPlayClientPlayerFlying flying,
                                                    LACLocation seed) {
        if (session == null || flying == null) {
            return Optional.empty();
        }
        Optional<LACMovementFrame> base = trackOrUpdate(session, flying, seed);
        if (!base.isPresent()) {
            return Optional.empty();
        }
        LACMovementFrame mf = base.get();
        if (inputEpoch != mf.getInputEpoch() || sequence != mf.getSequence()) {
            LACMovementFrame adjusted = new LACMovementFrame(
                    mf.getSession(), inputEpoch, sequence,
                    mf.getFrom(), mf.getTo(),
                    mf.isPositionChanged(), mf.isRotationChanged(),
                    mf.isClaimedOnGround(), mf.getReceivedAtMillis()
            );
            return Optional.of(adjusted);
        }
        return base;
    }

    public Optional<LACMovementFrame> trackOrUpdate(LACPlayerSession session,
                                                    long inputEpoch,
                                                    long sequence,
                                                    boolean hasPositionChanged,
                                                    boolean hasRotationChanged,
                                                    boolean isGround,
                                                    Location location,
                                                    LACLocation seed) {
        if (session == null || location == null) {
            return Optional.empty();
        }
        WrapperBackedFlying wrapper = new WrapperBackedFlying(
                hasPositionChanged, hasRotationChanged, isGround, location
        );
        return trackOrUpdateInternal(session, inputEpoch, sequence, wrapper, seed);
    }

    private Optional<LACMovementFrame> trackOrUpdateInternal(LACPlayerSession session,
                                                            long inputEpoch,
                                                            long sequence,
                                                            WrapperBackedFlying flying,
                                                            LACLocation seed) {
        if (session == null || flying == null) {
            return Optional.empty();
        }
        UUID playerId = session.getPlayerId();
        UUID worldId = session.getWorldId();
        long epoch = session.getPlayerEpoch();

        State state = states.get(playerId);
        if (state == null) {
            State created = new State();
            created.playerEpoch = epoch;
            created.worldId = worldId;
            LACLocation effectiveSeed = seed != null ? seed : seedFor(session);
            if (effectiveSeed == null) {
                Location loc = flying.getLocation();
                if (loc == null) return Optional.empty();
                effectiveSeed = new LACLocation(worldId, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
            if (!isFinite(effectiveSeed.getX()) || !isFinite(effectiveSeed.getY()) || !isFinite(effectiveSeed.getZ())
                    || !isFinite(effectiveSeed.getYaw()) || !isFinite(effectiveSeed.getPitch())) {
                return Optional.empty();
            }
            created.last = effectiveSeed;
            State prev = states.putIfAbsent(playerId, created);
            state = prev != null ? prev : created;
        }

        synchronized (state) {
            if (state.playerEpoch != epoch || !state.worldId.equals(worldId)) {
                LACLocation effectiveSeed = seed != null ? seed : seedFor(session);
                if (effectiveSeed == null) {
                    Location loc = flying.getLocation();
                    if (loc == null) {
                        states.remove(playerId);
                        return Optional.empty();
                    }
                    effectiveSeed = new LACLocation(worldId, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                }
                if (!isFinite(effectiveSeed.getX()) || !isFinite(effectiveSeed.getY()) || !isFinite(effectiveSeed.getZ())
                        || !isFinite(effectiveSeed.getYaw()) || !isFinite(effectiveSeed.getPitch())) {
                    states.remove(playerId);
                    return Optional.empty();
                }
                state.playerEpoch = epoch;
                state.worldId = worldId;
                state.last = effectiveSeed;
            }
            LACLocation from = state.last;
            if (from == null) return Optional.empty();
            Location raw = flying.getLocation();
            if (raw == null) return Optional.empty();

            double x = flying.hasPositionChanged() ? raw.getX() : from.getX();
            double y = flying.hasPositionChanged() ? raw.getY() : from.getY();
            double z = flying.hasPositionChanged() ? raw.getZ() : from.getZ();
            float yaw = flying.hasRotationChanged() ? raw.getYaw() : from.getYaw();
            float pitch = flying.hasRotationChanged() ? raw.getPitch() : from.getPitch();

            if (!isFinite(x) || !isFinite(y) || !isFinite(z) || !isFinite(yaw) || !isFinite(pitch)) {
                return Optional.empty();
            }

            LACLocation to = new LACLocation(worldId, x, y, z, yaw, pitch);
            long seq = sequence >= 0 ? sequence : state.packetSequence.incrementAndGet();
            long now = System.currentTimeMillis();
            LACMovementFrame frame = new LACMovementFrame(
                    session, inputEpoch, seq, from, to,
                    flying.hasPositionChanged(), flying.hasRotationChanged(),
                    flying.isOnGround(), now
            );
            state.last = to;
            if (sequence < 0) {
                // keep increment counter consistent
            } else {
                state.packetSequence.set(Math.max(state.packetSequence.get(), sequence));
            }
            return Optional.of(frame);
        }
    }

    private static final class WrapperBackedFlying {
        private final boolean hasPositionChanged;
        private final boolean hasRotationChanged;
        private final boolean onGround;
        private final Location location;

        WrapperBackedFlying(boolean hasPositionChanged, boolean hasRotationChanged, boolean onGround, Location location) {
            this.hasPositionChanged = hasPositionChanged;
            this.hasRotationChanged = hasRotationChanged;
            this.onGround = onGround;
            this.location = location;
        }

        boolean hasPositionChanged() {
            return hasPositionChanged;
        }

        boolean hasRotationChanged() {
            return hasRotationChanged;
        }

        boolean isOnGround() {
            return onGround;
        }

        Location getLocation() {
            return location;
        }
    }
}
