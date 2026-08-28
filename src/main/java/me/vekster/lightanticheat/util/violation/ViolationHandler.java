package me.vekster.lightanticheat.util.violation;

import me.vekster.lightanticheat.api.event.LACPunishmentEvent;
import me.vekster.lightanticheat.api.event.LACViolationEvent;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.CheckSetting;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.player.violation.PlayerViolations;
import me.vekster.lightanticheat.util.async.AsyncUtil;
import me.vekster.lightanticheat.util.config.ConfigManager;
import me.vekster.lightanticheat.util.config.placeholder.PlaceholderConvertor;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import me.vekster.lightanticheat.util.detection.LeanTowards;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import me.vekster.lightanticheat.util.command.RuntimeCommandDispatcher;
import me.vekster.lightanticheat.util.logger.LogType;
import me.vekster.lightanticheat.util.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViolationHandler implements Listener {

    private static final Set<CheckName> VERTICAL_SETBACK_CHECKS =
            Collections.unmodifiableSet(EnumSet.of(
                    CheckName.FLIGHT_A, CheckName.FLIGHT_B, CheckName.FLIGHT_C));

    private static final Set<CheckName> POST_VERTICAL_SETBACK_CHECKS =
            Collections.unmodifiableSet(EnumSet.of(
                    CheckName.SPEED_A, CheckName.SPEED_B, CheckName.SPEED_C, CheckName.JUMP_A, CheckName.JUMP_B));

    private static boolean isVerticalSetback(Player player, LACPlayer lacPlayer, CheckSetting checkSetting) {
        if (checkSetting.name.type != CheckName.CheckType.MOVEMENT)
            return false;
        if (CheckUtil.isOnGround(player, 0.2, lacPlayer.cache, LeanTowards.TRUE))
            return false;
        if (lacPlayer.cache.history.onEvent.onGround.get(HistoryElement.FIRST).towardsTrue)
            return false;

        boolean vSetback = VERTICAL_SETBACK_CHECKS.contains(checkSetting.name);
        boolean afterVSetback = false;
        if (POST_VERTICAL_SETBACK_CHECKS.contains(checkSetting.name)) {
            for (CheckName checkName : VERTICAL_SETBACK_CHECKS) {
                if (lacPlayer.violations.getViolations(checkName) < Math.min(5, checkSetting.punishmentVio / 2))
                    continue;
                afterVSetback = true;
                break;
            }
        }

        return vSetback || afterVSetback;
    }

    private static boolean isInsideSolidBlock(Player player, Location location) {
        for (Block block : CheckUtil.getWithinBlocks(player, location))
            if (!CheckUtil.isActuallyPassable(block))
                return true;
        return false;
    }

    private static boolean isSafeSetbackLocation(Player player, Location location) {
        return !isInsideSolidBlock(player, location);
    }

    private static Location getTopOfCurrentBlock(Location location) {
        Location result = location.clone();
        result.setY(Math.floor(result.getY()) + 1.0);
        return result;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFlag(LACViolationEvent event) {
        if (!event.getPlayer().isOnline() || event.getAcPlayer().leaveTime != 0L)
            return;
        if (ConfigManager.Config.Api.enabled && event.isCancelled())
            return;

        LACPlayer lacPlayer = event.getAcPlayer();
        CheckSetting checkSetting = event.getCheckSettings();

        if (checkSetting.punishmentVio == lacPlayer.violations.getViolations(checkSetting.name)) {
            Bukkit.getServer().getPluginManager().callEvent(new LACPunishmentEvent(event));
            return;
        }

        if (lacPlayer.violations.getViolations(checkSetting.name) < checkSetting.punishmentVio)
            lacPlayer.violations.increaseViolations(checkSetting.name, 1);

        long currentTime = System.currentTimeMillis();
        if (ConfigManager.Config.Log.enabled) {
            if (ConfigManager.Config.Log.LogViolations.enabled &&
                    lacPlayer.violations.tryAcquire(
                            PlayerViolations.NotificationChannel.VIOLATION_LOG,
                            currentTime,
                            ConfigManager.Config.Log.LogViolations.cooldown)) {
                Logger.logFile(PlaceholderConvertor.swapAll(ConfigManager.Config.Log.LogViolations.message,
                        checkSetting, event.getPlayer(), lacPlayer));
            }
        }

        if (ConfigManager.Config.Alerts.BroadcastViolations.enabled &&
                lacPlayer.violations.tryAcquire(
                        PlayerViolations.NotificationChannel.VIOLATION_ALERT,
                        currentTime,
                        ConfigManager.Config.Alerts.BroadcastViolations.cooldown)) {
            Logger.logAlert(ConfigManager.Config.Alerts.BroadcastViolations.message,
                    checkSetting, event.getPlayer(), lacPlayer);
        }

        if (ConfigManager.Config.DiscordWebhook.enabled) {
            if (ConfigManager.Config.DiscordWebhook.SendViolations.enabled &&
                    lacPlayer.violations.tryAcquire(
                            PlayerViolations.NotificationChannel.VIOLATION_DISCORD,
                            currentTime,
                            ConfigManager.Config.DiscordWebhook.SendViolations.cooldown)) {
                Logger.logDiscord(PlaceholderConvertor.swapAll(ConfigManager.Config.DiscordWebhook.SendViolations.message,
                        checkSetting, event.getPlayer(), lacPlayer), false);
            }
        }

        if (checkSetting.setback && lacPlayer.violations.getViolations(checkSetting.name) >= checkSetting.setbackVio &&
                event.getCancellable() != null) {
            if (!isVerticalSetback(event.getPlayer(), lacPlayer, checkSetting)) {
                event.getCancellable().setCancelled(true);
            } else {
                Location location = event.getPlayer().getLocation();
                boolean isDownBlocks = true;
                for (int i = 1; i <= 25; i++) {
                    boolean cancel = false;
                    Set<Block> blocks = new HashSet<>();
                    if (isDownBlocks || i == 25) {
                        blocks.addAll(new HashSet<>(CheckUtil.getDownBlocks(event.getPlayer(), location, 0.05)));
                    } else {
                        Block block = AsyncUtil.getBlock(location);
                        if (block != null) {
                            blocks.add(block);
                            blocks.add(block.getRelative(BlockFace.DOWN));
                        }
                    }
                    isDownBlocks = !isDownBlocks;
                    for (Block block : blocks)
                        if (!CheckUtil.isActuallyPassable(block) || i == 25) {
                            cancel = true;
                            break;
                        }
                    if (cancel) {
                        if (isInsideSolidBlock(event.getPlayer(), event.getPlayer().getLocation())) {
                            Location blockTopLocation = getTopOfCurrentBlock(location);
                            if (isSafeSetbackLocation(event.getPlayer(), blockTopLocation))
                                FoliaUtil.teleportPlayer(event.getPlayer(), blockTopLocation);
                        }
                        boolean slab = true;
                        for (Block block : CheckUtil.getDownBlocks(event.getPlayer(), 0.1)) {
                            if (!block.getType().name().endsWith("_SLAB")) {
                                slab = false;
                                break;
                            }
                        }
                        Location slabLocation = location.clone().subtract(0, 0.5, 0);
                        if (slab && isSafeSetbackLocation(event.getPlayer(), slabLocation))
                            FoliaUtil.teleportPlayer(event.getPlayer(), slabLocation);
                        break;
                    }
                    FoliaUtil.teleportPlayer(event.getPlayer(), location.subtract(0, 1, 0));
                }
            }
        }

        if (checkSetting.punishmentVio == lacPlayer.violations.getViolations(checkSetting.name))
            Bukkit.getServer().getPluginManager().callEvent(new LACPunishmentEvent(event));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPunishment(LACPunishmentEvent event) {
        if (!event.getPlayer().isOnline() || event.getAcPlayer().leaveTime != 0L)
            return;
        if (ConfigManager.Config.Api.enabled && event.isCancelled())
            return;

        LACPlayer lacPlayer = event.getAcPlayer();
        CheckSetting checkSetting = event.getCheckSettings();
        long currentTime = System.currentTimeMillis();
        if (ConfigManager.Config.Log.enabled) {
            if (ConfigManager.Config.Log.LogPunishments.enabled &&
                    lacPlayer.violations.tryAcquire(
                            PlayerViolations.NotificationChannel.PUNISHMENT_LOG,
                            currentTime,
                            ConfigManager.Config.Log.LogPunishments.cooldown)) {
                Logger.logFile(PlaceholderConvertor.swapAll(ConfigManager.Config.Log.LogPunishments.message,
                        checkSetting, event.getPlayer(), lacPlayer));
            }
        }

        if (ConfigManager.Config.Alerts.BroadcastPunishments.enabled &&
                lacPlayer.violations.tryAcquire(
                        PlayerViolations.NotificationChannel.PUNISHMENT_ALERT,
                        currentTime,
                        ConfigManager.Config.Alerts.BroadcastPunishments.cooldown)) {
            Logger.logAlert(ConfigManager.Config.Alerts.BroadcastPunishments.message,
                    checkSetting, event.getPlayer(), lacPlayer);
        }

        if (ConfigManager.Config.DiscordWebhook.enabled) {
            if (ConfigManager.Config.DiscordWebhook.SendPunishments.enabled &&
                    lacPlayer.violations.tryAcquire(
                            PlayerViolations.NotificationChannel.PUNISHMENT_DISCORD,
                            currentTime,
                            ConfigManager.Config.DiscordWebhook.SendPunishments.cooldown)) {
                Logger.logDiscord(PlaceholderConvertor.swapAll(ConfigManager.Config.DiscordWebhook.SendPunishments.message,
                        checkSetting, event.getPlayer(), lacPlayer), true);
            }
        }

        if (checkSetting.punishable && checkSetting.punishmentCommands != null && !checkSetting.punishmentCommands.isEmpty()) {
            final List<String> renderedCommands = new ArrayList<>(checkSetting.punishmentCommands.size());
            for (String command : checkSetting.punishmentCommands) {
                final String rendered = PlaceholderConvertor.renderPunishmentCommand(command, checkSetting, event.getPlayer(), lacPlayer);
                if (rendered == null || rendered.trim().isEmpty()) {
                    Logger.logConsole(LogType.ERROR, "(LightAntiCheat-Plus) Skipped empty punishment command for " + event.getPlayer().getName() + " (" + checkSetting.name.title + ")");
                    continue;
                }
                renderedCommands.add(rendered);
            }
            RuntimeCommandDispatcher.dispatchConsoleBatch(renderedCommands, event.getPlayer().getName(), checkSetting.name.title);
        }

        lacPlayer.violations = new PlayerViolations();
    }

}
