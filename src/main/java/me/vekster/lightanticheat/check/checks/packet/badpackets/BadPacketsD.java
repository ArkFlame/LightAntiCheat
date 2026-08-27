package me.vekster.lightanticheat.check.checks.packet.badpackets;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.checks.packet.PacketCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Impassible SetCreativeSlot packet
 */
public class BadPacketsD extends PacketCheck implements Listener {
    public BadPacketsD() {
        super(CheckName.BADPACKETS_D);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceive", event -> onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
    }

    public void onAsyncPacketReceive(LACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != LACPacketType.SET_CREATIVE_SLOT)
            return;

        Player player = event.getPlayer();
        LACPlayer lacPlayer = event.getLacPlayer();
        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (player.getGameMode() == GameMode.CREATIVE ||
                System.currentTimeMillis() - lacPlayer.cache.lastGamemodeChange < 500)
            return;

        Scheduler.runTaskLater(() -> {
            if (!player.isOnline() || lacPlayer.leaveTime != 0)
                return;

            if (player.getGameMode() == GameMode.CREATIVE ||
                    System.currentTimeMillis() - lacPlayer.cache.lastGamemodeChange < 500)
                return;

            flag(player, lacPlayer);
        }, 1);
    }

}
