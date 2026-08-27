package me.vekster.lightanticheat.check.checks.packet.morepackets;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.packet.PacketCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Packet limiter
 */
public class MorePacketsA extends PacketCheck implements Listener {
    public MorePacketsA() {
        super(CheckName.MOREPACKETS_A);
    }

    private static final int LONG_LIMIT = (int) Math.ceil(20 * 1.6 * 5.5);
    private static final int SHORT_LIMIT = (int) Math.ceil(20 * 0.8 * 4.5);

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceiveA", event -> onAsyncPacketReceiveA((LACAsyncPacketReceiveEvent) event));
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceiveB", event -> onAsyncPacketReceiveB((LACAsyncPacketReceiveEvent) event));
    }

    //      Long interval:

    public void onAsyncPacketReceiveA(LACAsyncPacketReceiveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (!limitPackets('A', buffer, 1600L, (int) Math.ceil(LONG_LIMIT * 1.9), 3))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 1500)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, lacPlayer, null);
        });
    }

    //      Short interval:

    public void onAsyncPacketReceiveB(LACAsyncPacketReceiveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();
        Buffer buffer = getBuffer(player, true);

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (!limitPackets('B', buffer, 800L, (int) Math.ceil(SHORT_LIMIT * 1.9), 5))
            return;

        if (System.currentTimeMillis() - buffer.getLong("lastFlagTime") < 700)
            return;
        buffer.put("lastFlagTime", System.currentTimeMillis());

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, lacPlayer, null);
        });
    }

}
