package me.vekster.lightanticheat.util.player.connectionstability;

import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventSubscriber;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionStabilityListener implements Listener, LACEventSubscriber {

    private static final Map<UUID, List<Integer>> PLAYERS = new ConcurrentHashMap<>();

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, this, "onAsyncMovement", event -> onAsyncMovement((LACAsyncPlayerMoveEvent) event));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PLAYERS.put(event.getPlayer().getUniqueId(), new LinkedList<>(Arrays.asList(0, 0, 0, 0)));
    }

    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!PLAYERS.containsKey(uuid))
            return;
        List<Integer> list = PLAYERS.get(uuid);
        list.set(list.size() - 1, list.get(list.size() - 1) + 1);
    }

    public static void loadConnectionCalculator() {
        Scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PLAYERS.entrySet().removeIf(entry -> {
                    if (Bukkit.getPlayer(entry.getKey()) == null)
                        return true;
                    List<Integer> list = entry.getValue();
                    list.add(0);
                    list.remove(0);
                    return false;
                });
            }
        }, 2000, 2000);
    }

    public static void loadConnectionCalculatorOnReload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PLAYERS.put(player.getUniqueId(), new LinkedList<>(Arrays.asList(0, 0, 0, 0)));
        }
    }

    public static ConnectionStability getConnectionStability(Player player) {
        UUID uuid = player.getUniqueId();
        if (!PLAYERS.containsKey(uuid))
            return ConnectionStability.HIGH;
        List<Integer> list = PLAYERS.get(uuid);
        int max = (int) Math.floor(list.stream()
                .mapToInt(v -> v)
                .max().orElseThrow(NoSuchElementException::new)
                / 2.0);
        if (max <= 25)
            return ConnectionStability.HIGH;
        else if (max <= 50)
            return ConnectionStability.MEDIUM;
        else
            return ConnectionStability.LOW;
    }

}
