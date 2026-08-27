package me.vekster.lightanticheat.listener.unloadedchunk;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventSubscriber;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.bus.LACMovementRequirement;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.blockcache.BlockMaterialCache;
import me.vekster.lightanticheat.util.async.AsyncUtil;
import me.vekster.lightanticheat.util.config.ConfigManager;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UnloadedChunkListener implements Listener, LACEventSubscriber {

    private static final Set<UUID> CHECKABLE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FROZEN_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void handleUnloadedChunks() {
        Bukkit.getPluginManager().registerEvents(new UnloadedChunkListener(), Main.getInstance());
        Scheduler.runTaskTimer(() -> {
            Scheduler.runTaskAsynchronously(true, () -> {
                CHECKABLE_PLAYERS.clear();
            });
        }, 10, 2);
        Scheduler.runTaskTimer(() -> {
            Scheduler.runTaskAsynchronously(true, () -> {
                FROZEN_PLAYERS.clear();
            });
        }, 10, 5);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_MOVE, LACEventPriority.NORMAL, this, "onAsyncMovement", LACMovementRequirement.POSITION, event -> onAsyncMovement((LACAsyncPlayerMoveEvent) event));
        LACEventBus.register(LACEventType.PLAYER_MOVE, LACEventPriority.NORMAL, this, "onMovement", LACMovementRequirement.POSITION, event -> onMovement((LACPlayerMoveEvent) event));
    }

    public void onAsyncMovement(LACAsyncPlayerMoveEvent event) {
        if (!ConfigManager.Config.LagProtection.preventEnteringIntoUnloadedChucks)
            return;
        UUID uuid = event.getPlayer().getUniqueId();

        if (FROZEN_PLAYERS.contains(uuid)) {
            CHECKABLE_PLAYERS.remove(uuid);
            event.setCancelled(true);
            return;
        }

        if (!event.isPlayerRiptiding() && !event.isPlayerGliding() &&
                CheckUtil.distanceHorizontal(event.getFrom(), event.getTo()) < 0.35)
            return;

        CHECKABLE_PLAYERS.add(uuid);
    }

    public void onMovement(LACPlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!CHECKABLE_PLAYERS.contains(player.getUniqueId()))
            return;
        Location location = player.getLocation();
        World world = AsyncUtil.getWorld(player);
        if (world == null) world = player.getWorld();
        if (BlockMaterialCache.isLoadedOwned(world, location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return;
        FROZEN_PLAYERS.add(player.getUniqueId());
    }

}
