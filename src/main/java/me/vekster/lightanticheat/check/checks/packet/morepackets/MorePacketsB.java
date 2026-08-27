package me.vekster.lightanticheat.check.checks.packet.morepackets;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.packet.PacketCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Nuker hack
 */
public class MorePacketsB extends PacketCheck implements Listener {
    public MorePacketsB() {
        super(CheckName.MOREPACKETS_B);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceive", event -> onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
    }

    public void onAsyncPacketReceive(LACAsyncPacketReceiveEvent event) {
        if (event.getPacketType() != LACPacketType.BLOCK_DIG)
            return;

        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (!limitPackets('A', buffer, 667L, 400, 3))
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, lacPlayer, null);
        });
    }

}
