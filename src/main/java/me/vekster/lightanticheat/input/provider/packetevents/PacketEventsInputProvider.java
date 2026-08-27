package me.vekster.lightanticheat.input.provider.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.vekster.lightanticheat.input.LACInputEngine;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.input.model.LACLocation;
import me.vekster.lightanticheat.input.model.LACMovementFrame;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.input.provider.LACInputProvider;
import me.vekster.lightanticheat.player.LACPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PacketEventsInputProvider implements LACInputProvider {

    private final LACInputEngine engine;
    private final PacketEventsMovementTracker tracker;
    private final Object lock = new Object();
    private volatile PacketListenerCommon listenerHandle;
    private volatile boolean started;

    public static boolean isPacketEventsAvailable() {
        try {
            Plugin pe = Bukkit.getPluginManager().getPlugin("packetevents");
            if (pe == null || !pe.isEnabled()) {
                return false;
            }
            Class<?> peClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            java.lang.reflect.Method getApi = peClass.getMethod("getAPI");
            Object api = getApi.invoke(null);
            if (api == null) {
                return false;
            }
            try {
                java.lang.reflect.Method isLoaded = api.getClass().getMethod("isLoaded");
                Object loaded = isLoaded.invoke(api);
                if (loaded instanceof Boolean && !((Boolean) loaded)) {
                    return false;
                }
            } catch (NoSuchMethodException ignored) {
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public PacketEventsInputProvider(LACInputEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.tracker = new PacketEventsMovementTracker();
    }

    PacketEventsInputProvider(LACInputEngine engine, PacketEventsMovementTracker tracker) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public LACInputMode getMode() {
        return LACInputMode.PACKET;
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            PacketListenerCommon listener = new PacketListenerAbstract(PacketListenerPriority.LOWEST) {
                @Override
                public void onPacketReceive(PacketReceiveEvent event) {
                    handleReceive(event);
                }

                @Override
                public void onUserDisconnect(UserDisconnectEvent event) {
                    User user = event.getUser();
                    if (user != null) {
                        UUID uuid = user.getUUID();
                        if (uuid != null) {
                            tracker.remove(uuid);
                            engine.remove(uuid);
                        }
                    }
                }
            };
            PacketListenerCommon registered = PacketEvents.getAPI().getEventManager().registerListener(listener);
            listenerHandle = registered != null ? registered : listener;
            started = true;
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (!started) {
                return;
            }
            PacketListenerCommon handle = listenerHandle;
            listenerHandle = null;
            started = false;
            tracker.clear();
            if (handle != null) {
                try {
                    PacketEvents.getAPI().getEventManager().unregisterListener(handle);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void handleReceive(PacketReceiveEvent event) {
        if (event == null) {
            return;
        }
        if (engine.getActiveMode() != LACInputMode.PACKET) {
            return;
        }
        PacketTypeCommon packetType = event.getPacketType();
        ConnectionState connectionState;
        try {
            connectionState = event.getConnectionState();
        } catch (Exception ignored) {
            connectionState = null;
        }
        if (connectionState != ConnectionState.PLAY && !(packetType instanceof PacketType.Play.Client)) {
            return;
        }
        if (!(packetType instanceof PacketType.Play.Client)) {
            return;
        }
        User user = event.getUser();
        if (user == null) {
            return;
        }
        UUID uuid;
        try {
            uuid = user.getUUID();
        } catch (Exception ignored) {
            return;
        }
        if (uuid == null) {
            return;
        }
        Optional<LACPlayerSession> sessionOpt = LACPlayerManager.captureSession(uuid);
        if (!sessionOpt.isPresent()) {
            return;
        }
        LACPlayerSession session = sessionOpt.get();
        PacketType.Play.Client clientType = (PacketType.Play.Client) packetType;
        LACPacketType mapped = PacketEventsPacketMapper.mapType(clientType);
        int entityId = PacketEventsPacketMapper.extractEntityId(event, mapped);

        long inputEpoch = session.getPlayerEpoch();
        long sequence = engine.nextSequence(session);

        Optional<LACMovementFrame> movementFrame = Optional.empty();
        Optional<LACLocation> packetMovementLocation = Optional.empty();

        if (mapped == LACPacketType.FLYING) {
            try {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                Optional<LACMovementFrame> mf = tracker.trackOrUpdate(session, inputEpoch, sequence, flying, null);
                if (mf.isPresent()) {
                    movementFrame = mf;
                    packetMovementLocation = Optional.of(mf.get().getTo());
                }
            } catch (Exception ignored) {
                movementFrame = Optional.empty();
            }
        }

        long now = System.currentTimeMillis();
        LACPacketFrame frame = new LACPacketFrame(
                session, inputEpoch, sequence, mapped, entityId, packetMovementLocation, now
        );
        engine.enqueue(frame, movementFrame);
    }

    // exposed for testing
    PacketEventsMovementTracker getTracker() {
        return tracker;
    }

    PacketListenerCommon getListenerHandle() {
        return listenerHandle;
    }
}
