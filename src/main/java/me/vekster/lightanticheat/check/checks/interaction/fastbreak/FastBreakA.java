package me.vekster.lightanticheat.check.checks.interaction.fastbreak;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.interaction.InteractionCheck;
import me.vekster.lightanticheat.event.playerbreakblock.LACPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.AureliumSkillsHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.EnchantsSquaredHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.McMMOHook;
import me.vekster.lightanticheat.util.hook.plugin.simplehook.VeinMinerHook;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Mining with a pickaxe using Timer or FastBreak hack
 */
public class FastBreakA extends InteractionCheck implements Listener {

    private static final Material DEEPSLATE = VerUtil.material.get("DEEPSLATE");

    private static final Map<Material, MiningDuration> DURATIONS = createDurations(false);
    private static final Map<Material, MiningDuration> ENCHANTED_DURATIONS = createDurations(true);

    private static Map<Material, MiningDuration> createDurations(boolean enchanted) {
        EnumMap<Material, MiningDuration> map = new EnumMap<>(Material.class);
        if (enchanted) {
            putIfPresent(map, VerUtil.material.get("WOODEN_PICKAXE"), new MiningDuration(100, 200));
            putIfPresent(map, Material.STONE_PICKAXE, new MiningDuration(100, 150));
            putIfPresent(map, Material.IRON_PICKAXE, new MiningDuration(100, 150));
            putIfPresent(map, Material.DIAMOND_PICKAXE, new MiningDuration(100, 150));
            putIfPresent(map, VerUtil.material.get("NETHERITE_PICKAXE"), new MiningDuration(100, 150));
        } else {
            putIfPresent(map, VerUtil.material.get("WOODEN_PICKAXE"), new MiningDuration(1150, 2250));
            putIfPresent(map, Material.STONE_PICKAXE, new MiningDuration(600, 1150));
            putIfPresent(map, Material.IRON_PICKAXE, new MiningDuration(400, 750));
            putIfPresent(map, Material.DIAMOND_PICKAXE, new MiningDuration(300, 600));
            putIfPresent(map, VerUtil.material.get("NETHERITE_PICKAXE"), new MiningDuration(250, 500));
        }
        return Collections.unmodifiableMap(map);
    }

    private static void putIfPresent(Map<Material, MiningDuration> map, Material material, MiningDuration duration) {
        if (material != null)
            map.put(material, duration);
    }

    private static final class MiningDuration {
        private final int stoneMillis;
        private final int deepslateMillis;

        MiningDuration(int stoneMillis, int deepslateMillis) {
            this.stoneMillis = stoneMillis;
            this.deepslateMillis = deepslateMillis;
        }

        int forMaterial(Material material) {
            if (material == Material.STONE)
                return stoneMillis;
            if (material == DEEPSLATE)
                return deepslateMillis;
            return 0;
        }
    }

    public FastBreakA() {
        super(CheckName.FASTBREAK_A);
    }

    @EventHandler
    public void onBlockBreak(LACPlayerBreakBlockEvent event) {
        Player player = event.getPlayer();
        LACPlayer lacPlayer = event.getLacPlayer();

        if (!isCheckAllowed(player, lacPlayer))
            return;
        if (player.getGameMode() != GameMode.SURVIVAL)
            return;
        Block block = event.getBlock();
        if (AureliumSkillsHook.isPrevented(player) ||
                VeinMinerHook.isPrevented(player) ||
                McMMOHook.isPrevented(block.getType()))
            return;

        Buffer buffer = getBuffer(player);
        ItemStack tool = lacPlayer.getItemInMainHand();
        if (tool == null) return;
        int efficiencyLevel = tool.getEnchantmentLevel(VerUtil.enchantment.get("EFFICIENCY"));
        if (efficiencyLevel > 5)
            return;
        boolean enchantedTool = efficiencyLevel != 0;

        if (block.getType() != Material.STONE && block.getType() != VerUtil.material.get("DEEPSLATE")) {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }
        if (DURATIONS.get(tool.getType()) == null) {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("lastInteraction")) {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("blockType") || buffer.getMaterial("blockType") != block.getType()) {
            buffer.put("blockType", block.getType());
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("tool") || buffer.getMaterial("tool") != tool.getType()) {
            buffer.put("tool", tool.getType());
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        if (!buffer.isExists("enchantedTool") || buffer.getBoolean("enchantedTool") != enchantedTool) {
            buffer.put("enchantedTool", enchantedTool);
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - buffer.getLong("effectTime") <= 10 * 1000) {
            buffer.put("flags", 0);
            return;
        }

        long interval = System.currentTimeMillis() - buffer.getLong("lastInteraction");

        long maxDuration = (enchantedTool ? ENCHANTED_DURATIONS : DURATIONS).get(tool.getType()).forMaterial(block.getType());

        boolean flag = interval < maxDuration / 1.45;

        if (flag) {
            if (buffer.getInt("flags") < 6)
                buffer.put("flags", buffer.getInt("flags") + 1);
        } else {
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
            if (buffer.getInt("flags") > 0)
                buffer.put("flags", buffer.getInt("flags") - 1);
        }

        if (buffer.getInt("flags") < 6)
            return;
        if (buffer.getInt("flags") > 0)
            buffer.put("flags", buffer.getInt("flags") - 1);
        if (buffer.getInt("flags") > 0)
            buffer.put("flags", buffer.getInt("flags") - 1);

        if (EnchantsSquaredHook.hasEnchantment(player, "Excavation", "Deforestation", "Harvesting"))
            return;

        Map<String, Double> attributes = getPlayerAttributes(player);
        if (getItemStackAttributes(player, "PLAYER_BLOCK_BREAK_SPEED",
                "PLAYER_MINING_EFFICIENCY", "PLAYER_SUBMERGED_MINING_SPEED") != 0 ||
                attributes.getOrDefault("PLAYER_BLOCK_BREAK_SPEED", 0.0) > 0.01 ||
                attributes.getOrDefault("PLAYER_MINING_EFFICIENCY", 0.0) > 0.01 ||
                attributes.getOrDefault("PLAYER_SUBMERGED_MINING_SPEED", 0.0) > 0.01)
            buffer.put("attribute", System.currentTimeMillis());
        if (System.currentTimeMillis() - buffer.getLong("attribute") < 3500)
            return;

        callViolationEvent(player, lacPlayer, event.getEvent());
    }

    @EventHandler
    public void onInteraction(PlayerInteractEvent event) {
        if (isExternalNPC(event)) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Buffer buffer = getBuffer(event.getPlayer());
        buffer.put("lastInteraction", System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void beforeBlockBreak(LACPlayerBreakBlockEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, lacPlayer))
            return;

        if (getEffectAmplifier(player, VerUtil.potions.get("FAST_DIGGING")) > 0) {
            Buffer buffer = getBuffer(player);
            buffer.put("effectTime", System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMovement(LACAsyncPlayerMoveEvent event) {
        LACPlayer lacPlayer = event.getLacPlayer();
        Player player = event.getPlayer();

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (getEffectAmplifier(lacPlayer.cache, VerUtil.potions.get("FAST_DIGGING")) > 0) {
            Scheduler.runTask(true, () -> {
                Buffer buffer = getBuffer(player);
                buffer.put("effectTime", System.currentTimeMillis());
            });
        }
    }

}
