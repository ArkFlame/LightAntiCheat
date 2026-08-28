package me.vekster.lightanticheat.util.config.placeholder;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.CheckSetting;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceholderConvertorPunishmentCommandTest {

    static {
        installMockServerAndMain();
    }

    private static void installMockServerAndMain() {
        if (Bukkit.getServer() == null) {
            InvocationHandler handler = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("isPrimaryThread".equals(name)) return Boolean.TRUE;
                    if ("getBukkitVersion".equals(name)) return "1.20.4-R0.1-SNAPSHOT";
                    if ("getName".equals(name)) return "TestServer";
                    if ("getVersion".equals(name)) return "1.20.4";
                    Class<?> returnType = method.getReturnType();
                    if (returnType.isPrimitive()) {
                        if (returnType == boolean.class) return Boolean.FALSE;
                        if (returnType == char.class) return Character.valueOf('\0');
                        if (returnType == long.class) return Long.valueOf(0);
                        if (returnType == double.class) return Double.valueOf(0);
                        if (returnType == float.class) return Float.valueOf(0);
                        if (returnType == short.class) return Short.valueOf((short) 0);
                        if (returnType == byte.class) return Byte.valueOf((byte) 0);
                        return Integer.valueOf(0);
                    }
                    if (returnType.isInterface()) {
                        return Proxy.newProxyInstance(
                                returnType.getClassLoader(),
                                new Class<?>[]{returnType},
                                this);
                    }
                    return null;
                }
            };
            Server server = (Server) Proxy.newProxyInstance(
                    Server.class.getClassLoader(),
                    new Class<?>[]{Server.class},
                    handler);
            try {
                Field serverField = Bukkit.class.getDeclaredField("server");
                serverField.setAccessible(true);
                serverField.set(null, server);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
        if (Main.getInstance() == null) {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field f = unsafeClass.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                Object unsafe = f.get(null);
                Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
                Main mockMain = (Main) allocate.invoke(unsafe, Main.class);
                PluginDescriptionFile desc = new PluginDescriptionFile("LightAntiCheat", "me.vekster.lightanticheat.Main", "2.0.1");
                Field descField = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredField("description");
                descField.setAccessible(true);
                descField.set(mockMain, desc);
                Field instanceField = Main.class.getDeclaredField("instance");
                instanceField.setAccessible(true);
                instanceField.set(null, mockMain);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    @BeforeEach
    void clearRegistry() throws Exception {
        Field field = LACPlayerManager.class.getDeclaredField("PLAYERS");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    private static final class WorldHolder {
        final UUID uid = UUID.randomUUID();
    }

    private static World newWorldProxy(WorldHolder holder) {
        InvocationHandler h = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                switch (method.getName()) {
                    case "getUID": return holder.uid;
                    case "getName": return "world";
                    case "getEnvironment": return World.Environment.NORMAL;
                    case "toString": return "world";
                    default: {
                        Class<?> rt = method.getReturnType();
                        if (rt.isPrimitive()) {
                            if (rt == boolean.class) return false;
                            if (rt == char.class) return '\0';
                            if (rt == void.class) return null;
                            return 0;
                        }
                        return null;
                    }
                }
            }
        };
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class[]{World.class}, h);
    }

    private static final class Fixture {
        final CheckSetting checkSetting;
        final Player player;
        final LACPlayer lacPlayer;
        Fixture(CheckSetting cs, Player p, LACPlayer lp) { this.checkSetting = cs; this.player = p; this.lacPlayer = lp; }
    }

    private static Fixture newFixture() {
        CheckSetting cs = new CheckSetting(CheckName.FLIGHT_A);
        World world = newWorldProxy(new WorldHolder());
        final UUID uuid = UUID.randomUUID();
        InvocationHandler playerHandler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                switch (method.getName()) {
                    case "getUniqueId": return uuid;
                    case "getWorld": return world;
                    case "getLocation": return new Location(world, 0.0, 64.0, 0.0);
                    case "getAddress": return null;
                    case "getName": return "Sinsajox";
                    case "isOnline": return true;
                    case "toString": return "Sinsajox";
                    case "equals": return proxy == args[0];
                    case "hashCode": return System.identityHashCode(proxy);
                    default: {
                        Class<?> rt = method.getReturnType();
                        if (rt.isPrimitive()) {
                            if (rt == boolean.class) return false;
                            if (rt == char.class) return '\0';
                            if (rt == void.class) return null;
                            if (rt == long.class) return 0L;
                            if (rt == double.class) return 0.0;
                            if (rt == float.class) return 0.0f;
                            if (rt == short.class) return (short) 0;
                            if (rt == byte.class) return (byte) 0;
                            return 0;
                        }
                        return null;
                    }
                }
            }
        };
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class}, playerHandler);
        try {
            Constructor<LACPlayer> ctor = LACPlayer.class.getDeclaredConstructor(Player.class);
            ctor.setAccessible(true);
            LACPlayer lac = ctor.newInstance(player);
            Field mapField = LACPlayerManager.class.getDeclaredField("PLAYERS");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, LACPlayer> map = (Map<UUID, LACPlayer>) mapField.get(null);
            map.put(uuid, lac);
            return new Fixture(cs, player, lac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void legacyVanillaKickTargetRemovesOneStar() {
        assertEquals("kick Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("kick *Sinsajox FlightB", "Sinsajox"));
    }

    @Test
    public void legacyNamespacedVanillaKickTargetRemovesOneStar() {
        assertEquals("minecraft:kick Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("minecraft:kick *Sinsajox FlightB", "Sinsajox"));
    }

    @Test
    public void legacyUppercaseKickLabelStillNormalizes() {
        assertEquals("KICK Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("KICK *Sinsajox FlightB", "Sinsajox"));
    }

    @Test
    public void normalVanillaKickTargetRemainsUnchanged() {
        assertEquals("kick Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("kick Sinsajox FlightB", "Sinsajox"));
    }

    @Test
    public void nonKickCommandWithStarTargetRemainsUnchanged() {
        assertEquals("ban *Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("ban *Sinsajox FlightB", "Sinsajox"));
        assertEquals("tempban *Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("tempban *Sinsajox FlightB", "Sinsajox"));
    }

    @Test
    public void differentPlayerStarTargetRemainsUnchanged() {
        assertEquals("kick *OtherPlayer FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("kick *OtherPlayer FlightB", "Sinsajox"));
    }

    @Test
    public void reasonAndSpacingAfterTargetRemainUnchanged() {
        assertEquals("kick Sinsajox    FlightB  extra",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("kick *Sinsajox    FlightB  extra", "Sinsajox"));
    }

    @Test
    public void nullCommandReturnsEmptyString() {
        assertEquals("",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget(null, "Sinsajox"));
    }

    @Test
    public void emptyCommandReturnsEmptyString() {
        assertEquals("",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("", "Sinsajox"));
    }

    @Test
    public void emptyPlayerNameLeavesCommandUnchanged() {
        assertEquals("kick *Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("kick *Sinsajox FlightB", ""));
        assertEquals("kick *Sinsajox FlightB",
                PlaceholderConvertor.normalizeLegacyVanillaKickTarget("kick *Sinsajox FlightB", null));
    }

    @Test
    public void leadingSlashIsRemovedForBukkitDispatch() {
        Fixture f = newFixture();
        assertEquals("kick Sinsajox FlightB",
                PlaceholderConvertor.renderPunishmentCommand("/kick Sinsajox FlightB", f.checkSetting, f.player, f.lacPlayer));
    }

    @Test
    public void outerWhitespaceIsRemovedForBukkitDispatch() {
        Fixture f = newFixture();
        assertEquals("minecraft:kick Sinsajox  FlightB",
                PlaceholderConvertor.renderPunishmentCommand("  /minecraft:kick Sinsajox  FlightB  ", f.checkSetting, f.player, f.lacPlayer));
    }

    @Test
    public void namespacedLeadingSlashIsRemoved() {
        Fixture f = newFixture();
        assertEquals("minecraft:kick Sinsajox FlightB",
                PlaceholderConvertor.renderPunishmentCommand("/minecraft:kick Sinsajox FlightB", f.checkSetting, f.player, f.lacPlayer));
    }

    @Test
    public void doubleSlashRemovesOnlyOneSlash() {
        Fixture f = newFixture();
        assertEquals("/custom arg",
                PlaceholderConvertor.renderPunishmentCommand("//custom arg", f.checkSetting, f.player, f.lacPlayer));
    }

    @Test
    public void slashOnlyBecomesEmpty() {
        Fixture f = newFixture();
        assertEquals("",
                PlaceholderConvertor.renderPunishmentCommand("/", f.checkSetting, f.player, f.lacPlayer));
        assertEquals("",
                PlaceholderConvertor.renderPunishmentCommand("   ", f.checkSetting, f.player, f.lacPlayer));
    }

    @Test
    public void internalArgumentSpacingIsPreserved() {
        Fixture f = newFixture();
        assertEquals("kick A  B",
                PlaceholderConvertor.renderPunishmentCommand("kick A  B", f.checkSetting, f.player, f.lacPlayer));
        assertEquals("kick Sinsajox  FlightB",
                PlaceholderConvertor.renderPunishmentCommand("kick Sinsajox  FlightB", f.checkSetting, f.player, f.lacPlayer));
    }
}
