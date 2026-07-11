package me.vekster.lightanticheat.version;

import me.vekster.lightanticheat.Main;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.annotation.SecureAsync;
import me.vekster.lightanticheat.util.cooldown.CooldownUtil;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import me.vekster.lightanticheat.util.logger.LogType;
import me.vekster.lightanticheat.util.logger.Logger;
import me.vekster.lightanticheat.util.reflection.ReflectionException;
import me.vekster.lightanticheat.util.reflection.ReflectionUtil;
import me.vekster.lightanticheat.version.identifier.LACVersion;
import me.vekster.lightanticheat.version.identifier.VerIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class VerPlayer {

    private static final Map<String, Boolean> CACHE = new HashMap<>();
    private static final Map<String, Boolean> ASYNC_CACHE = new ConcurrentHashMap<>();
    private static Class<?> craftPlayerClass;
    private volatile Player player;

    static {
        try {
            craftPlayerClass = ReflectionUtil.classForName("org.bukkit.craftbukkit.$version.entity.CraftPlayer");
        } catch (ReflectionException e) {
            Logger.logConsole(LogType.ERROR, "(" + Main.getInstance().getName() + ") CraftPlayer class is not found!");
        }
    }

    public VerPlayer(Player player) {
        bindPlayer(player);
    }

    protected final void bindPlayer(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    protected final Player boundPlayer() {
        return Objects.requireNonNull(this.player, "player");
    }

    public static int getPingWithoutCache(Player player, boolean async) {
        String methodName = "getPing";
        Map<String, Boolean> cache = !async ? CACHE : ASYNC_CACHE;

        if (!cache.containsKey(methodName)) {
            try {
                cache.put(methodName, ReflectionUtil.runDeclaredMethod(player, methodName) != null);
            } catch (ReflectionException e) {
                cache.put(methodName, false);
            }
        }

        try {
            if (cache.get(methodName)) {
                Object value = ReflectionUtil.runDeclaredMethod(player, methodName);
                if (value instanceof Integer)
                    return (int) value;
                return 250;
            }

            if (craftPlayerClass == null) return 0;
            Object craftPlayer = craftPlayerClass.cast(player);
            Object entityPlayer = ReflectionUtil.runDeclaredMethod(craftPlayer, "getHandle");
            if (entityPlayer == null) return 0;
            Object result = ReflectionUtil.getDeclaredField(entityPlayer, "ping");
            if (result instanceof Integer)
                return (int) result;
        } catch (ReflectionException e) {
            return 250;
        }
        return 250;
    }

    public static int getPing(Player player) {
        return CooldownUtil.getPing(LACPlayer.getLacPlayer(player).cooldown, player, false);
    }

    @SecureAsync
    public static int getPing(Player player, boolean async) {
        return CooldownUtil.getPing(LACPlayer.getLacPlayer(player).cooldown, player, async);
    }

    public int getPing() {
        final Player player = boundPlayer();
        return CooldownUtil.getPing(LACPlayer.getLacPlayer(player).cooldown, player, false);
    }

    @SecureAsync
    public int getPing(boolean async) {
        final Player player = boundPlayer();
        return CooldownUtil.getPing(LACPlayer.getLacPlayer(player).cooldown, player, async);
    }

    @SecureAsync
    public static boolean isGliding(Player player) {
        return VerUtil.multiVersion.isGliding(player) ||
                VerUtil.multiVersion.isGlidingToggled(player);
    }

    @SecureAsync
    public boolean isGliding() {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.isGliding(player) ||
                VerUtil.multiVersion.isGlidingToggled(player);
    }

    @SecureAsync
    public static boolean isRiptiding(Player player) {
        return VerUtil.multiVersion.isRiptiding(player);
    }

    @SecureAsync
    public boolean isRiptiding() {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.isRiptiding(player);
    }

    @SecureAsync
    public static boolean isSwimming(Player player) {
        return VerUtil.multiVersion.isSwimming(player);
    }

    @SecureAsync
    public boolean isSwimming() {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.isSwimming(player);
    }

    @SecureAsync
    public static boolean isClimbing(Player player) {
        if (!FoliaUtil.isFolia()) return VerUtil.multiVersion.isClimbing(player);
        return LACPlayerManager.current(player).map(ctx -> ctx.cache().playerClimbing).orElse(false);
    }

    @SecureAsync
    public boolean isClimbing() {
        return VerPlayer.isClimbing(boundPlayer());
    }

    @SecureAsync
    public static boolean isInWater(Player player) {
        if (!FoliaUtil.isFolia()) return VerUtil.multiVersion.isInWater(player);
        return LACPlayerManager.current(player).map(ctx -> ctx.cache().playerInWater).orElse(false);
    }

    @SecureAsync
    public boolean isInWater() {
        return VerPlayer.isInWater(boundPlayer());
    }

    @SecureAsync
    public static ItemStack getItemInMainHand(Player player) {
        return VerUtil.multiVersion.getItemInMainHand(player);
    }

    @SecureAsync
    public ItemStack getItemInMainHand() {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.getItemInMainHand(player);
    }

    @SecureAsync
    public static ItemStack getItemInOffHand(Player player) {
        return VerUtil.multiVersion.getItemInOffHand(player);
    }

    @SecureAsync
    public ItemStack getItemInOffHand() {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.getItemInOffHand(player);
    }

    @SecureAsync
    @Nullable
    public static Block getTargetBlockExact(Player player, int distance) {
        return VerUtil.multiVersion.getTargetBlockExact(player, distance);
    }

    @SecureAsync
    @Nullable
    public Block getTargetBlockExact(int distance) {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.getTargetBlockExact(player, distance);
    }

    @SecureAsync
    public static void sendBlockDate(Player player, Location location, Block block) {
        VerUtil.multiVersion.sendBlockData(player, location, block);
    }

    @SecureAsync
    public void sendBlockDate(Location location, Block block) {
        final Player player = boundPlayer();
        VerUtil.multiVersion.sendBlockData(player, location, block);
    }

    @SecureAsync
    public static boolean sendHoverMessage(Player player, List<String> lines, boolean hexColor) {
        return VerUtil.multiVersion.sendHoverMessage(player, lines, hexColor);
    }

    @SecureAsync
    public boolean sendHoverMessage(List<String> lines, boolean hexColor) {
        final Player player = boundPlayer();
        return VerUtil.multiVersion.sendHoverMessage(player, lines, hexColor);
    }

    @NotNull
    public ItemStack getArmorPiece(EquipmentSlot equipmentSlot) {
        final Player player = boundPlayer();
        return VerUtil.getArmorPiece(player.getInventory(), equipmentSlot);
    }

}
