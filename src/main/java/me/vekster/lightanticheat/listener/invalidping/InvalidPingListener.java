package me.vekster.lightanticheat.listener.invalidping;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventSubscriber;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetreceive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class InvalidPingListener implements Listener, LACEventSubscriber {

    public static void limitMaxPing() {
        Bukkit.getPluginManager().registerEvents(new InvalidPingListener(), Main.getInstance());
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PACKET_RECEIVE, LACEventPriority.NORMAL, this, "onAsyncPacketReceive", event -> onAsyncPacketReceive((LACAsyncPacketReceiveEvent) event));
    }

    public void onAsyncPacketReceive(LACAsyncPacketReceiveEvent event) {
        if (event.getLacPlayer().getPing(true) <= 10000)
            return;
        Scheduler.runTask(true, () -> {
            if (!event.getPlayer().isOnline())
                return;
            event.getPlayer().kickPlayer("Internal Exception: java.net.SocketException: Connection Reset");
        });
    }

}
