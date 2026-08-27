package me.vekster.lightanticheat.check.checks.packet.badpackets;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.checks.packet.PacketCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.player.LACPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Impossible entity ID
 */
public class BadPacketsB extends PacketCheck implements Listener {
    public BadPacketsB() {
        super(CheckName.BADPACKETS_B);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceive", event -> onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
    }

    public void onAsyncPacketReceive(LACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != LACPacketType.USE_ENTITY)
            return;

        Player player = event.getPlayer();
        LACPlayer lacPlayer = event.getLacPlayer();
        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (event.getEntityId() < 0)
            flag(player, lacPlayer);
    }

}
