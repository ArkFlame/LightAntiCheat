package me.vekster.lightanticheat.util.hook.plugin;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.annotation.SecureAsync;
import me.vekster.lightanticheat.util.config.ConfigManager;
import me.vekster.lightanticheat.util.cooldown.CooldownUtil;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.UUID;

public class FloodgateHook {

    private static PluginManager pluginManager;
    private static final String PLUGIN_NAME = "floodgate";
    private static boolean enabled;
    private static boolean available;
    private static Object floodgateApi;
    private static Method isFloodgatePlayerMethod;
    private static Method getPlayerMethod;
    private static Method getDeviceOsMethod;
    public static void loadFloodgateHook() {
        pluginManager = Main.getInstance().getServer().getPluginManager();
        reload();
    }

    public static void reload() {
        enabled = ConfigManager.Config.GeyserHook.Floodgate.enabled;
        loadFloodgateApi();
    }

    public static boolean isBedrockPlayerWithoutCache(Player player) {
        if (!ConfigManager.Config.GeyserHook.enabled)
            return false;
        if (ConfigManager.Config.GeyserHook.UUID.enabled &&
                player.getUniqueId().toString().startsWith("000000"))
            return true;
        final String configuredPrefix = ConfigManager.Config.GeyserHook.Prefix.prefixString;
        if (ConfigManager.Config.GeyserHook.Prefix.enabled && configuredPrefix != null && !configuredPrefix.isEmpty()
                && player.getName().startsWith(configuredPrefix))
            return true;

        if (!enabled || pluginManager.getPlugin(PLUGIN_NAME) == null)
            return false;
        if (available) {
            try {
                final Object result = isFloodgatePlayerMethod.invoke(floodgateApi, player.getUniqueId());
                return result instanceof Boolean && (Boolean) result;
            } catch (final ReflectiveOperationException exception) {
                available = false;
                Main.getInstance().getLogger().warning("Floodgate reflection hook failed. Falling back to prefix/UUID detection.");
            }
        }
        final String prefix = ConfigManager.Config.GeyserHook.Prefix.prefixString;
        return prefix != null && !prefix.isEmpty() && player.getName().startsWith(prefix);
    }

    public static boolean isBedrockPlayer(Player player) {
        return CooldownUtil.isBedrockPlayer(LACPlayer.getLacPlayer(player).cooldown, player);
    }

    @SecureAsync
    public static boolean isBedrockPlayer(Player player, boolean async) {
        return CooldownUtil.isBedrockPlayer(LACPlayer.getLacPlayer(player).cooldown, player, async);
    }

    @SecureAsync
    public static boolean isProbablyPocketEditionPlayer(Player player, boolean async) {
        if (!isBedrockPlayer(player, async))
            return false;

        if (!enabled || pluginManager.getPlugin(PLUGIN_NAME) == null)
            return true;
        if (!available)
            return true;
        try {
            final Object result = isFloodgatePlayerMethod.invoke(floodgateApi, player.getUniqueId());
            if (!(result instanceof Boolean) || !(Boolean) result)
                return true;
            final Object floodgatePlayer = getPlayerMethod.invoke(floodgateApi, player.getUniqueId());
            if (floodgatePlayer == null)
                return true;
            if (getDeviceOsMethod == null) {
                getDeviceOsMethod = floodgatePlayer.getClass().getMethod("getDeviceOs");
            }
            final Object device = getDeviceOsMethod.invoke(floodgatePlayer);
            if (device == null)
                return true;
            String deviceOs = device.toString();
            if (deviceOs.equals("UNKNOWN") || deviceOs.equals("GOOGLE") || deviceOs.equals("IOS") ||
                    deviceOs.equals("AMAZON") || deviceOs.equals("GEARVR") || deviceOs.equals("TVOS") ||
                    deviceOs.equals("PS4") || deviceOs.equals("NX") || deviceOs.equals("XBOX") ||
                    deviceOs.equals("WINDOWS_PHONE"))
                return true;
            return false;
        } catch (final ReflectiveOperationException exception) {
            available = false;
            Main.getInstance().getLogger().warning("Floodgate device lookup failed. Falling back to UNKNOWN.");
            return true;
        }
    }

    public static boolean isProbablyPocketEditionPlayer(Player player) {
        return isProbablyPocketEditionPlayer(player, false);
    }

    @SecureAsync
    public static boolean isCancelledCombat(CheckName checkName, Player player, boolean async) {
        if (checkName != CheckName.KILLAURA_B &&
                checkName != CheckName.REACH_A && checkName != CheckName.REACH_B)
            return false;
        if (!isProbablyPocketEditionPlayer(player, async))
            return false;
        return true;
    }

    @SecureAsync
    public static boolean isCancelledMovement(CheckName checkName, Player player, boolean async) {
        if (!isProbablyPocketEditionPlayer(player, async))
            return false;
        if (checkName == CheckName.SPEED_B) {
            for (Block block : CheckUtil.getDownBlocks(player, 0.12))
                if (block.getType().name().endsWith("_STAIRS"))
                    return true;
        }
        if (checkName == CheckName.STEP_A) {
            for (Block block : CheckUtil.getDownBlocks(player, 0.12))
                if (block.getType().name().endsWith("_STAIRS"))
                    return true;
            for (Block block : CheckUtil.getCollisionBlockLayer(player))
                if (block.getType().name().endsWith("_STAIRS") ||
                        block.getRelative(BlockFace.UP).getType().name().endsWith("_STAIRS"))
                    return true;
        }
        return false;
    }

    private static void loadFloodgateApi() {
        available = false;
        floodgateApi = null;
        isFloodgatePlayerMethod = null;
        getPlayerMethod = null;
        getDeviceOsMethod = null;

        if (!enabled || pluginManager.getPlugin(PLUGIN_NAME) == null) {
            return;
        }
        try {
            final Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            final Method getInstanceMethod = apiClass.getMethod("getInstance");
            floodgateApi = getInstanceMethod.invoke(null);
            isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            getPlayerMethod = apiClass.getMethod("getPlayer", UUID.class);
            available = floodgateApi != null;
        } catch (final ReflectiveOperationException exception) {
            available = false;
            Main.getInstance().getLogger().warning("Floodgate plugin is installed but API reflection failed. Falling back to prefix/UUID detection.");
        }
    }
}
