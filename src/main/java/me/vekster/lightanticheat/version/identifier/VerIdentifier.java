package me.vekster.lightanticheat.version.identifier;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VerIdentifier {

    private static final Pattern BUKKIT_MINOR_PATTERN = Pattern.compile("^1\\.(\\d+)");
    private static LACVersion serverVersion = null;

    public static LACVersion getVersion() {
        if (serverVersion != null)
            return serverVersion;

        serverVersion = parseBukkitVersion(Bukkit.getBukkitVersion());
        if (serverVersion != null)
            return serverVersion;

        serverVersion = parsePackageVersion(Bukkit.getServer().getClass().getPackage().getName());
        if (serverVersion != null)
            return serverVersion;

        serverVersion = LACVersion.V1_21;
        return serverVersion;
    }

    private static LACVersion parseBukkitVersion(String bukkitVersion) {
        if (bukkitVersion == null)
            return null;
        Matcher matcher = BUKKIT_MINOR_PATTERN.matcher(bukkitVersion);
        if (!matcher.find())
            return null;
        try {
            return fromMinor(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static LACVersion parsePackageVersion(String packageName) {
        if (packageName == null || packageName.isEmpty())
            return null;
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        if (version.startsWith("v1_8")) return LACVersion.V1_8;
        if (version.startsWith("v1_9")) return LACVersion.V1_9;
        if (version.startsWith("v1_10")) return LACVersion.V1_10;
        if (version.startsWith("v1_11")) return LACVersion.V1_11;
        if (version.startsWith("v1_12")) return LACVersion.V1_12;
        if (version.startsWith("v1_13")) return LACVersion.V1_13;
        if (version.startsWith("v1_14")) return LACVersion.V1_14;
        if (version.startsWith("v1_15")) return LACVersion.V1_15;
        if (version.startsWith("v1_16")) return LACVersion.V1_16;
        if (version.startsWith("v1_17")) return LACVersion.V1_17;
        if (version.startsWith("v1_18")) return LACVersion.V1_18;
        if (version.startsWith("v1_19")) return LACVersion.V1_19;
        if (version.startsWith("v1_20")) return LACVersion.V1_20;
        if (version.startsWith("v1_21")) return LACVersion.V1_21;
        return null;
    }

    private static LACVersion fromMinor(int minor) {
        if (minor <= 8) return LACVersion.V1_8;
        if (minor == 9) return LACVersion.V1_9;
        if (minor == 10) return LACVersion.V1_10;
        if (minor == 11) return LACVersion.V1_11;
        if (minor == 12) return LACVersion.V1_12;
        if (minor == 13) return LACVersion.V1_13;
        if (minor == 14) return LACVersion.V1_14;
        if (minor == 15) return LACVersion.V1_15;
        if (minor == 16) return LACVersion.V1_16;
        if (minor == 17) return LACVersion.V1_17;
        if (minor == 18) return LACVersion.V1_18;
        if (minor == 19) return LACVersion.V1_19;
        if (minor == 20) return LACVersion.V1_20;
        return LACVersion.V1_21;
    }

}
