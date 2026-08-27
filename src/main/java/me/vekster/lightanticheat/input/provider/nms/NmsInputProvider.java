package me.vekster.lightanticheat.input.provider.nms;

import com.fren_gor.lightInjector.LightInjector;
import io.netty.channel.Channel;
import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.input.LACInputEngine;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import me.vekster.lightanticheat.input.provider.LACInputProvider;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.config.ConfigManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class NmsInputProvider extends LightInjector implements LACInputProvider {

    private final LACInputEngine engine;
    private final Object lock = new Object();
    private volatile boolean started;

    public NmsInputProvider(@NotNull Main plugin, @NotNull LACInputEngine engine) {
        super(Objects.requireNonNull(plugin, "plugin"));
        this.engine = Objects.requireNonNull(engine, "engine");
        this.started = true;
    }

    @Override
    public LACInputMode getMode() {
        return LACInputMode.NMS;
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            if (isClosed()) {
                return;
            }
            started = true;
        }
    }

    @Override
    public boolean isStarted() {
        return started && !isClosed();
    }

    @Override
    protected @Nullable Object onPacketReceiveAsync(@Nullable Player sender, @NotNull Channel channel, @NotNull Object nmsPacket) {
        if (sender == null) {
            return nmsPacket;
        }
        try {
            if (!ConfigManager.Config.enabled) {
                return nmsPacket;
            }
        } catch (Exception ignored) {
        }
        if (engine.getActiveMode() != LACInputMode.NMS) {
            return nmsPacket;
        }
        Optional<LACPlayerSession> sessionOpt;
        try {
            sessionOpt = LACPlayerManager.captureSession(sender.getUniqueId());
        } catch (Exception e) {
            return nmsPacket;
        }
        if (!sessionOpt.isPresent()) {
            return nmsPacket;
        }
        LACPlayerSession session = sessionOpt.get();

        NmsPacketRecognizer.Result result;
        try {
            result = NmsPacketRecognizer.recognize(nmsPacket);
        } catch (Exception e) {
            return nmsPacket;
        }

        LACPacketType mapped = result.getPacketType();
        int entityId = result.getEntityId();

        long inputEpoch = session.getPlayerEpoch();
        long sequence = engine.nextSequence(session);
        long now = System.currentTimeMillis();

        LACPacketFrame frame = new LACPacketFrame(
                session,
                inputEpoch,
                sequence,
                mapped,
                entityId,
                Optional.empty(),
                now
        );
        try {
            engine.enqueue(frame, Optional.empty());
        } catch (Exception ignored) {
        }
        return nmsPacket;
    }

    @Override
    protected @Nullable Object onPacketSendAsync(@Nullable Player receiver, @NotNull Channel channel, @NotNull Object nmsPacket) {
        return nmsPacket;
    }
}
