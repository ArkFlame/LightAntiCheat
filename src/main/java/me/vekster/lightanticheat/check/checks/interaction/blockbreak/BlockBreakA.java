package me.vekster.lightanticheat.check.checks.interaction.blockbreak;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.interaction.InteractionCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.playerbreakblock.LACAsyncPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACPlayerBreakBlockEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.player.cache.history.PlayerCacheHistory;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.AureliumSkillsHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.McMMOHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.VeinMinerHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Head rotation
 */
public class BlockBreakA extends InteractionCheck implements Listener {
    public BlockBreakA() {
        super(CheckName.BLOCKBREAK_A);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, this, "onAsyncBlockBreak", event -> onAsyncBlockBreak((LACAsyncPlayerBreakBlockEvent) event));
        LACEventBus.register(LACEventType.PLAYER_BREAK_BLOCK, LACEventPriority.NORMAL, this, "onBlockBreak", event -> onBlockBreak((LACPlayerBreakBlockEvent) event));
    }

    public void onAsyncBlockBreak(LACAsyncPlayerBreakBlockEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        buffer.put("lastAsyncResult", flag(event.getPlayer(), event.getLacPlayer(),
                event.getBlock(), event.getEyeLocation(), true));
    }

    public void onBlockBreak(LACPlayerBreakBlockEvent event) {
        Buffer buffer = getBuffer(event.getPlayer(), true);
        if (!buffer.getBoolean("lastAsyncResult"))
            return;
        if (!flag(event.getPlayer(), event.getLacPlayer(),
                event.getBlock(), event.getPlayer().getEyeLocation(), false))
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("lastFlag") < 500)
            return;
        buffer.put("lastFlag", currentTime);

        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 1)
            return;

        Block block = event.getBlock();

        LACPlayerManager.executeLater(event.getContext(), 1L, context -> {
            if (block.getWorld() == null || !context.worldId().equals(block.getWorld().getUID()))
                return;

            Player player = context.player();
            LACPlayer lacPlayer = context.owner();

            if (getYawChange(player.getEyeLocation(), lacPlayer) > 30.0)
                return;

            if (AureliumSkillsHook.isPrevented(player) ||
                    VeinMinerHook.isPrevented(player) ||
                    McMMOHook.isPrevented(block.getType()))
                return;

            if (EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting"))
                return;

            callViolationEvent(player, lacPlayer, null);
        });
    }

    private boolean flag(Player player, LACPlayer lacPlayer, Block block, Location eyeLocation, boolean async) {
        if (!isCheckAllowed(player, lacPlayer, async))
            return false;

        boolean flag = true;
        Location blockLocation = block.getLocation();
        Block targetBlock = lacPlayer.getTargetBlockExact(10);
        if (targetBlock != null)
            if (distanceHorizontal(blockLocation, targetBlock.getLocation()) <= 3.5)
                flag = false;

        if (flag) {
            Set<Material> transparent = new HashSet<>();
            transparent.add(Material.AIR);
            transparent.add(Material.WATER);
            transparent.add(Material.LAVA);
            transparent.add(block.getType());
            if (targetBlock != null)
                transparent.add(targetBlock.getType());

            List<Block> lineOfSight = player.getLineOfSight(transparent, 10);
            for (Block block1 : lineOfSight) {
                if (distanceHorizontal(blockLocation, block1.getLocation()) <= 3.0) {
                    flag = false;
                    break;
                }
            }
        }

        Vector vector = blockLocation.toVector().setY(0.0D).subtract(eyeLocation.toVector().setY(0.0D));
        float angle = eyeLocation.getDirection().setY(0.0D).angle(vector) * 57.2958F;
        if (angle > 120 && eyeLocation.getPitch() < 60 && eyeLocation.getPitch() > -40)
            flag = true;
        return flag;
    }

    private static double getYawChange(Location eyeLocation, LACPlayer lacPlayer) {
        float yaw = yaw(eyeLocation.getYaw());
        PlayerCacheHistory<Location> eventHistory = lacPlayer.cache.history.onEvent.location;
        PlayerCacheHistory<Location> packetHistory = lacPlayer.cache.history.onPacket.location;
        return Math.max(
                Math.min(Math.abs(yaw - yaw(eventHistory.get(HistoryElement.FROM).getYaw())),
                        Math.abs(yaw - yaw(packetHistory.get(HistoryElement.FROM).getYaw()))),
                Math.min(Math.abs(yaw - yaw(eventHistory.get(HistoryElement.FIRST).getYaw())),
                        Math.abs(yaw - yaw(packetHistory.get(HistoryElement.FIRST).getYaw())))
        );
    }

    private static float yaw(float yaw) {
        yaw = yaw % 360;
        return yaw >= 0 ? yaw : 360 - yaw;
    }

}
