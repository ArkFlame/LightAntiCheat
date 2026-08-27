package me.vekster.lightanticheat.check.checks.packet.timer;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.packet.PacketCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.bus.LACMovementRequirement;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.plugin.FloodgateHook;
import me.vekster.lightanticheat.version.identifier.LACVersion;
import me.vekster.lightanticheat.version.identifier.VerIdentifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Timer hack
 */
public class TimerA extends PacketCheck implements Listener {
    public TimerA() {
        super(CheckName.TIMER_A);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceive", event -> onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, this, "onAsyncMovement", LACMovementRequirement.POSITION, event -> onAsyncMovement((LACAsyncPlayerMoveEvent) event));
    }

    public void onAsyncPacketReceive(LACAsyncPacketReceiveEvent event) {
        Player player = event.getPlayer();
        LACPlayer lacPlayer = event.getLacPlayer();

        if (event.getPacketType() != LACPacketType.FLYING)
            return;

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (FloodgateHook.isProbablyPocketEditionPlayer(player, true))
            return;

        Buffer buffer = getBuffer(player, true);
        long currentTime = System.currentTimeMillis();

        if (!buffer.getBoolean("moved") || currentTime - lacPlayer.joinTime < 2000)
            return;

        if (VerIdentifier.getVersion().isOlderOrEqualsTo(LACVersion.V1_8) && currentTime - lacPlayer.joinTime < 12000)
            return;

        if (player.isInsideVehicle()) {
            buffer.put("skipVehiclePacket", !buffer.getBoolean("skipVehiclePacket"));
            if (!buffer.getBoolean("skipVehiclePacket"))
                return;
        }

        if (System.currentTimeMillis() - lacPlayer.cache.lastWindCharge < 3000 ||
                System.currentTimeMillis() - lacPlayer.cache.lastWindChargeReceive < 1000) {
            buffer.put("skipVehiclePacket", !buffer.getBoolean("skipVehiclePacket"));
            if (!buffer.getBoolean("skipVehiclePacket"))
                return;
        }

        if (!buffer.isExists("lastTime") || !buffer.isExists("packets") ||
                !buffer.isExists("packetsBalancer") || !buffer.isExists("balancerTime"))
            buffer.put("lastNonExistingFieldTime", currentTime);

        long difference = currentTime - buffer.getLong("lastTime");
        buffer.put("packets", buffer.getInt("packets") + 1);
        buffer.put("packetsBalancer", buffer.getInt("packetsBalancer") + 1);
        if (difference >= 1000) {
            buffer.put("lastTime", currentTime);
            buffer.put("balancerTime", buffer.getInt("balancerTime") + (21 - buffer.getInt("packetsBalancer")));
            buffer.put("packets", 0);
            buffer.put("packetsBalancer", 0);
            return;
        }
        if (buffer.getInt("balancerTime") > 0) {
            buffer.put("balancerTime", buffer.getInt("balancerTime") - 1);
            buffer.put("packets", buffer.getInt("packets") - 1);
            return;
        }
        if (buffer.getInt("packets") > (VerIdentifier.getVersion().isNewerThan(LACVersion.V1_8) ? 28 : 35)) {
            if (currentTime - lacPlayer.joinTime > 10 * 1000)
                localFlag(buffer, player, lacPlayer);
            buffer.put("packets", buffer.getInt("packets") - 2);
        }
    }

    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        if (distance(event.getFrom(), event.getTo()) == 0)
            return;
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("moved", true);
    }

    private void localFlag(Buffer buffer, Player player, LACPlayer lacPlayer) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastNonExistingFieldTime") <= 2000)
            return;

        if (currentTime - buffer.getLong("localFlagTime") > 2000) {
            buffer.put("localFlagTime", currentTime);
            buffer.put("localFlags", 0);
        }
        buffer.put("localFlags", buffer.getInt("localFlags") + 1);
        if (buffer.getInt("localFlags") <= 2)
            return;

        if (currentTime - buffer.getLong("lastFlagTime") <= 2000)
            return;

        flag(player, lacPlayer);
        buffer.put("lastFlagTime", currentTime);
    }

}
