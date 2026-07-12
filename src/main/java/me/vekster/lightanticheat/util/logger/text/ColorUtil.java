package me.vekster.lightanticheat.util.logger.text;

import me.vekster.lightanticheat.util.config.ConfigManager;
import org.bukkit.ChatColor;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[&§][a-zA-Z0-9]");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("[&§]#[a-fA-F0-9]{6}");

    private static final Method CHAT_COLOR_OF = resolveChatColorOf();
    private static final AtomicBoolean CHAT_COLOR_DISABLED = new AtomicBoolean(false);

    private static final class PaletteEntry {
        final char code;
        final ChatColor color;

        PaletteEntry(char code, ChatColor color) {
            this.code = code;
            this.color = color;
        }
    }

    private static final PaletteEntry[] LEGACY_PALETTE = new PaletteEntry[]{
            new PaletteEntry('0', ChatColor.BLACK),
            new PaletteEntry('1', ChatColor.DARK_BLUE),
            new PaletteEntry('2', ChatColor.DARK_GRAY),
            new PaletteEntry('3', ChatColor.DARK_AQUA),
            new PaletteEntry('4', ChatColor.DARK_RED),
            new PaletteEntry('5', ChatColor.DARK_PURPLE),
            new PaletteEntry('6', ChatColor.GOLD),
            new PaletteEntry('7', ChatColor.GRAY),
            new PaletteEntry('8', ChatColor.DARK_GRAY),
            new PaletteEntry('9', ChatColor.BLUE),
            new PaletteEntry('a', ChatColor.GREEN),
            new PaletteEntry('b', ChatColor.AQUA),
            new PaletteEntry('c', ChatColor.RED),
            new PaletteEntry('d', ChatColor.LIGHT_PURPLE),
            new PaletteEntry('e', ChatColor.YELLOW),
            new PaletteEntry('f', ChatColor.WHITE)
    };

    private static final int[] LEGACY_PALETTE_RGB = new int[]{
            0x000000,
            0x0000AA,
            0x00AA00,
            0x00AAAA,
            0xAA0000,
            0xAA00AA,
            0xFFAA00,
            0xAAAAAA,
            0x555555,
            0x5555FF,
            0x55FF55,
            0x55FFFF,
            0xFF5555,
            0xFF55FF,
            0xFFFF55,
            0xFFFFFF
    };

    public static String colorize(String text, boolean customColor) {
        if (!text.contains("&"))
            return text;
        if (!ConfigManager.Config.Messages.hexColorCodes)
            customColor = false;

        Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group().substring(1);
            String replacement = customColor
                    ? String.valueOf(chatColorOf(hex))
                    : String.valueOf(getBukkitColor(hex));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        text = sb.toString();

        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', text);
    }

    public static net.md_5.bungee.api.ChatColor chatColorOf(String hex) {
        if (CHAT_COLOR_DISABLED.get() || CHAT_COLOR_OF == null)
            return getBukkitColor(hex);
        try {
            Object result = CHAT_COLOR_OF.invoke(net.md_5.bungee.api.ChatColor.class, hex);
            if (result instanceof net.md_5.bungee.api.ChatColor)
                return (net.md_5.bungee.api.ChatColor) result;
            return getBukkitColor(hex);
        } catch (IllegalAccessException | InvocationTargetException e) {
            CHAT_COLOR_DISABLED.compareAndSet(false, true);
            return getBukkitColor(hex);
        }
    }

    private static Method resolveChatColorOf() {
        try {
            return net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
        } catch (NoSuchMethodException e) {
            CHAT_COLOR_DISABLED.compareAndSet(false, true);
            return null;
        }
    }

    public static net.md_5.bungee.api.ChatColor getBukkitColor(String hex) {
        Color hexColor;
        try {
            hexColor = Color.decode(hex);
        } catch (IllegalArgumentException e) {
            return net.md_5.bungee.api.ChatColor.BLACK;
        }
        ChatColor closestColor = ChatColor.BLACK;
        double closestDistance = 1000.0;
        for (int i = 0; i < LEGACY_PALETTE.length; i++) {
            double distance = colorDistance(hexColor, LEGACY_PALETTE_RGB[i]);
            if (distance >= closestDistance)
                continue;
            closestColor = LEGACY_PALETTE[i].color;
            closestDistance = distance;
        }
        return closestColor.asBungee();
    }

    private static double colorDistance(Color c1, int rgb2) {
        int red1 = c1.getRed();
        int red2 = (rgb2 >> 16) & 0xFF;
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = c1.getGreen() - ((rgb2 >> 8) & 0xFF);
        int b = c1.getBlue() - (rgb2 & 0xFF);
        return Math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8));
    }

    public static String removeColors(String message) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = LEGACY_COLOR_PATTERN.matcher(message);
        while (matcher.find())
            matcher.appendReplacement(sb, "");
        matcher.appendTail(sb);
        String stripped = sb.toString();

        sb.setLength(0);
        matcher = LEGACY_HEX_PATTERN.matcher(stripped);
        while (matcher.find())
            matcher.appendReplacement(sb, "");
        matcher.appendTail(sb);
        return sb.toString();
    }

}
