package me.vekster.lightanticheat.check.checks.player.skinblinker;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.player.PlayerCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.bus.LACMovementRequirement;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.plugin.FloodgateHook;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * SkinBlinker hack
 */
public class SkinBlinkerA extends PlayerCheck implements Listener {
    public SkinBlinkerA() {
        super(CheckName.SKINBLINKER_A);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceive", event -> onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, this, "onMovement", LACMovementRequirement.POSITION_AND_ROTATION, event -> onMovement((LACAsyncPlayerMoveEvent) event));
    }

    public void onAsyncPacketReceive(LACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != LACPacketType.CLIENT_INFORMATION)
            return;

        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (FloodgateHook.isBedrockPlayer(player, true))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastMovement") < 333)
            buffer.put("packets", buffer.getInt("packets") + 1);

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("startTime") <= 2000)
            return;
        buffer.put("startTime", currentTime);

        int packets = buffer.getInt("packets");
        buffer.put("packets", 0);

        if (packets < 12) {
            buffer.put("flags", 0);
            return;
        }
        buffer.put("flags", buffer.getInt("flags") + 1);

        if (buffer.getInt("flags") < 2)
            return;
        buffer.put("flags", 0);

        if (System.currentTimeMillis() - buffer.getLong("lastMovement") >= 1800)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, lacPlayer, null);
        });
    }

    public void onMovement(LACAsyncPlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (Math.abs(from.getYaw()) - to.getYaw() <= 5 &&
                Math.abs(from.getPitch()) - to.getPitch() <= 0.5)
            return;

        if (distance(event.getFrom(), event.getTo()) == 0)
            return;

        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastMovement", System.currentTimeMillis());
    }

}
