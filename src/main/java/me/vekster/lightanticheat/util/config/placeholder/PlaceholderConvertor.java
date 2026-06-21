package me.vekster.lightanticheat.util.config.placeholder;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.check.CheckSetting;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.async.AsyncUtil;
import me.vekster.lightanticheat.util.config.ConfigManager;
import me.vekster.lightanticheat.util.hook.plugin.FloodgateHook;
import me.vekster.lightanticheat.util.logger.text.ColorUtil;
import me.vekster.lightanticheat.util.player.brand.ClientBrandRecognizer;
import me.vekster.lightanticheat.util.player.connectionstability.ConnectionStability;
import me.vekster.lightanticheat.util.player.connectionstability.ConnectionStabilityListener;
import me.vekster.lightanticheat.util.tps.TPSCalculator;
import me.vekster.lightanticheat.version.VerPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class PlaceholderConvertor {

    private static final SimpleDateFormat SEC_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat MIN_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat HRS_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH");
    private static final SimpleDateFormat DAY_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd");

    public static String colorize(String text, boolean customColor) {
        return ColorUtil.colorize(text, customColor);
    }

    public static String replacePlaceholder(String text, String placeholder, Object value) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace(placeholder, Objects.toString(value, ""));
    }

    public static String swapAll(String text, CheckSetting checkSetting, Player player, LACPlayer lacPlayer) {
        long currentTime = System.currentTimeMillis();
        InetSocketAddress address = player.getAddress();
        text = swapConnectionStability(text, player);
        text = swapCoordinates(text, player, "#0.00");
        text = replacePlaceholder(text, "%prefix%", ConfigManager.Config.Messages.prefix);
        text = replacePlaceholder(text, "%version%", Main.getInstance().getDescription().getVersion());
        text = replacePlaceholder(text, "%check%", checkSetting.name.title);
        text = replacePlaceholder(text, "%check-type%", checkSetting.name.type.toString().toUpperCase().charAt(0)
                + checkSetting.name.type.toString().toLowerCase().substring(1));
        text = replacePlaceholder(text, "%check-description%", checkSetting.name.description);
        text = replacePlaceholder(text, "%vio%", String.valueOf(lacPlayer.violations.getViolations(checkSetting.name)));
        text = replacePlaceholder(text, "%punishment-vio%", String.valueOf(checkSetting.punishmentVio));
        text = replacePlaceholder(text, "%setback-vio%", String.valueOf(checkSetting.setbackVio));
        text = replacePlaceholder(text, "%tps%", formatDecimalFraction("#0.00", Math.min(TPSCalculator.getTPS(), 20.0)));
        text = replacePlaceholder(text, "%ping%", String.valueOf(VerPlayer.getPing(player)));
        text = replacePlaceholder(text, "%edition%", FloodgateHook.isBedrockPlayer(player) ? "Bedrock" : "Java");
        text = replacePlaceholder(text, "%name%", player.getName());
        text = replacePlaceholder(text, "%uuid%", player.getUniqueId().toString());
        text = replacePlaceholder(text, "%ip%", address != null ? address.getAddress().toString().substring(1) : "none");
        text = replacePlaceholder(text, "%client-brand%", ClientBrandRecognizer.getClientBrand(player));
        text = replacePlaceholder(text, "%date-sec%", SEC_FORMAT.format(new Date(currentTime)));
        text = replacePlaceholder(text, "%date-min%", MIN_FORMAT.format(new Date(currentTime)));
        text = replacePlaceholder(text, "%date-hrs%", HRS_FORMAT.format(new Date(currentTime)));
        text = replacePlaceholder(text, "%date-day%", DAY_FORMAT.format(new Date(currentTime)));
        return text;
    }

    public static String swapSome(String text, Placeholder... placeholders) {
        for (Placeholder placeholder : placeholders) {
            switch (placeholder) {
                case PREFIX:
                    text = replacePlaceholder(text, "%prefix%", ConfigManager.Config.Messages.prefix);
                    break;
                case VERSION:
                    text = replacePlaceholder(text, "%version%", Main.getInstance().getDescription().getVersion());
                    break;
                case TPS:
                    text = replacePlaceholder(text, "%tps%", new DecimalFormat("#0.00")
                            .format(Math.min(TPSCalculator.getTPS(), 20.0)));
                    break;
                case DATA:
                    long time = System.currentTimeMillis();
                    text = replacePlaceholder(text, "%date-sec%", SEC_FORMAT.format(new Date(time)));
                    text = replacePlaceholder(text, "%date-min%", MIN_FORMAT.format(new Date(time)));
                    text = replacePlaceholder(text, "%date-hrs%", HRS_FORMAT.format(new Date(time)));
                    text = replacePlaceholder(text, "%date-day%", DAY_FORMAT.format(new Date(time)));
                    break;
            }
        }
        return text;
    }

    private static String formatDecimalFraction(String format, double fraction) {
        return new DecimalFormat(format).format(fraction);
    }

    private static String swapConnectionStability(String text, Player player) {
        ConnectionStability connectionStability = ConnectionStabilityListener.getConnectionStability(player);
        if (connectionStability == ConnectionStability.LOW)
            return replacePlaceholder(text, "%connection-stability%", ConfigManager.Config.Messages.CommandMessages.Ping.ConnectionStability.low);
        if (connectionStability == ConnectionStability.MEDIUM)
            return replacePlaceholder(text, "%connection-stability%", ConfigManager.Config.Messages.CommandMessages.Ping.ConnectionStability.medium);
        text = replacePlaceholder(text, "%connection-stability%", ConfigManager.Config.Messages.CommandMessages.Ping.ConnectionStability.high);
        return text;
    }

    public static String swapCoordinates(String text, Player player, String format) {
        Location location = player.getLocation();
        World world = AsyncUtil.getWorld(player);
        if (world == null) world = player.getWorld();
        text = replacePlaceholder(text, "%world%", world.getName());
        text = replacePlaceholder(text, "%x%", formatDecimalFraction(format, location.getX()));
        text = replacePlaceholder(text, "%y%", formatDecimalFraction(format, location.getY()));
        text = replacePlaceholder(text, "%z%", formatDecimalFraction(format, location.getZ()));
        return text;
    }

    public static String swapPlayer(String text, Player player) {
        InetSocketAddress address = player.getAddress();
        text = swapConnectionStability(text, player);
        text = swapCoordinates(text, player, "#0.00");
        text = replacePlaceholder(text, "%ping%", String.valueOf(VerPlayer.getPing(player)));
        text = replacePlaceholder(text, "%name%", player.getName());
        text = replacePlaceholder(text, "%uuid%", player.getUniqueId().toString());
        text = replacePlaceholder(text, "%ip%", address != null ? address.getAddress().toString().substring(1) : "none");
        text = replacePlaceholder(text, "%client-brand%", ClientBrandRecognizer.getClientBrand(player));
        return text;
    }

}
