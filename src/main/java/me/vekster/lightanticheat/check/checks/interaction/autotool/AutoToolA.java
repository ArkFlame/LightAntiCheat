package me.vekster.lightanticheat.check.checks.interaction.autotool;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.interaction.InteractionCheck;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Automated tool swap
 */
public class AutoToolA extends InteractionCheck implements Listener {

    private static final long MAX_SWAP_DELAY = 150L;
    private static final long BACK_SWITCH_DELAY = 150L;
    private static final long STREAK_WINDOW = 5000L;
    private static final int SWITCH_THRESHOLD = 2;

    public AutoToolA() {
        super(CheckName.AUTOTOOL_A);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (isExternalNPC(event)) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        Player player = event.getPlayer();
        LACPlayer lacPlayer = LACPlayer.getLacPlayer(player);
        Block clickedBlock = event.getClickedBlock();
        Material clickedType = clickedBlock.getType();
        int clickedSlot = player.getInventory().getHeldItemSlot();
        Material heldType = getItemType(player.getInventory().getItem(clickedSlot));

        Scheduler.entityThread(player, () -> {
            if (!isCheckAllowed(player, lacPlayer))
                return;
            if (!isSurvivalLike(player.getGameMode()))
                return;

            Buffer buffer = getBuffer(player);
            buffer.put("lastClickTime", System.currentTimeMillis());
            buffer.put("lastBlockType", clickedType);
            buffer.put("lastSlot", clickedSlot);
            buffer.put("lastHeldType", heldType);
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (isExternalNPC(event)) return;

        Player player = event.getPlayer();
        LACPlayer lacPlayer = LACPlayer.getLacPlayer(player);

        Scheduler.entityThread(player, () -> {
            if (!isCheckAllowed(player, lacPlayer))
                return;
            if (!isSurvivalLike(player.getGameMode()))
                return;

            Buffer buffer = getBuffer(player);
            long currentTime = System.currentTimeMillis();
            if (isBackSwitch(buffer, event, currentTime)) {
                buffer.put("originalSlot", -1);
                callViolationEvent(player, lacPlayer, event);
                return;
            }

            long lastClickTime = buffer.getLong("lastClickTime");
            if (lastClickTime == 0L)
                return;

            long delay = currentTime - lastClickTime;
            if (delay > MAX_SWAP_DELAY) {
                decreaseSwitches(buffer);
                return;
            }
            if (event.getPreviousSlot() != buffer.getInt("lastSlot"))
                return;

            Material blockType = buffer.getMaterial("lastBlockType");
            Material previousTool = buffer.getMaterial("lastHeldType");
            Material newTool = getItemType(player.getInventory().getItem(event.getNewSlot()));
            if (isCorrectTool(previousTool, blockType) || !isCorrectTool(newTool, blockType)) {
                decreaseSwitches(buffer);
                return;
            }

            if (currentTime - buffer.getLong("lastSuspiciousSwitch") > STREAK_WINDOW)
                buffer.put("correctSwitches", 0);
            buffer.put("lastSuspiciousSwitch", currentTime);

            int switches = buffer.getInt("correctSwitches") + 1;
            buffer.put("correctSwitches", switches);
            if (switches < SWITCH_THRESHOLD)
                return;

            buffer.put("lastCorrectSwap", currentTime);
            buffer.put("originalSlot", event.getPreviousSlot());
            callViolationEvent(player, lacPlayer, event);
        });
    }

    private static boolean isBackSwitch(Buffer buffer, PlayerItemHeldEvent event, long currentTime) {
        int originalSlot = buffer.getInt("originalSlot");
        return originalSlot >= 0 &&
                event.getNewSlot() == originalSlot &&
                currentTime - buffer.getLong("lastCorrectSwap") <= BACK_SWITCH_DELAY;
    }

    private static void decreaseSwitches(Buffer buffer) {
        if (buffer.getInt("correctSwitches") > 0)
            buffer.put("correctSwitches", buffer.getInt("correctSwitches") - 1);
    }

    private static boolean isSurvivalLike(GameMode gameMode) {
        return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
    }

    private static Material getItemType(ItemStack itemStack) {
        if (itemStack == null)
            return Material.AIR;
        return itemStack.getType();
    }

    private static boolean isCorrectTool(Material tool, Material block) {
        if (tool == null || block == null || tool == Material.AIR || block == Material.AIR)
            return false;

        String toolName = tool.name();
        String blockName = block.name();
        if (toolName.equals("SHEARS"))
            return isShearsBlock(blockName);
        if (toolName.endsWith("_PICKAXE"))
            return isPickaxeBlock(blockName);
        if (toolName.endsWith("_AXE"))
            return isAxeBlock(blockName);
        if (toolName.endsWith("_SHOVEL") || toolName.endsWith("_SPADE"))
            return isShovelBlock(blockName);
        if (toolName.endsWith("_HOE"))
            return isHoeBlock(blockName);
        return false;
    }

    private static boolean isPickaxeBlock(String blockName) {
        return blockName.contains("STONE") ||
                blockName.contains("ORE") ||
                blockName.contains("DEEPSLATE") ||
                blockName.contains("BLACKSTONE") ||
                blockName.contains("BASALT") ||
                blockName.contains("TUFF") ||
                blockName.contains("CALCITE") ||
                blockName.contains("PRISMARINE") ||
                blockName.contains("TERRACOTTA") ||
                blockName.contains("CONCRETE") ||
                blockName.contains("BRICK") ||
                blockName.contains("COPPER") ||
                blockName.equals("NETHERRACK") ||
                blockName.equals("END_STONE") ||
                blockName.equals("OBSIDIAN") ||
                blockName.equals("CRYING_OBSIDIAN") ||
                blockName.equals("ANCIENT_DEBRIS") ||
                blockName.equals("IRON_BLOCK") ||
                blockName.equals("GOLD_BLOCK") ||
                blockName.equals("DIAMOND_BLOCK") ||
                blockName.equals("EMERALD_BLOCK") ||
                blockName.equals("REDSTONE_BLOCK") ||
                blockName.equals("LAPIS_BLOCK") ||
                blockName.equals("COAL_BLOCK") ||
                blockName.equals("NETHERITE_BLOCK") ||
                blockName.equals("QUARTZ_BLOCK") ||
                blockName.equals("PURPUR_BLOCK");
    }

    private static boolean isAxeBlock(String blockName) {
        return blockName.contains("LOG") ||
                blockName.contains("WOOD") ||
                blockName.contains("STEM") ||
                blockName.contains("HYPHAE") ||
                blockName.contains("PLANKS") ||
                blockName.contains("FENCE") ||
                blockName.contains("DOOR") ||
                blockName.contains("SIGN") ||
                blockName.contains("CHEST") ||
                blockName.contains("BAMBOO") ||
                blockName.equals("BARREL") ||
                blockName.equals("CRAFTING_TABLE") ||
                blockName.equals("BOOKSHELF") ||
                blockName.equals("LECTERN") ||
                blockName.equals("LADDER") ||
                blockName.equals("BEEHIVE") ||
                blockName.equals("BEE_NEST") ||
                blockName.equals("PUMPKIN") ||
                blockName.equals("MELON") ||
                blockName.equals("COCOA") ||
                blockName.equals("JUKEBOX") ||
                blockName.equals("NOTE_BLOCK");
    }

    private static boolean isShovelBlock(String blockName) {
        return blockName.contains("DIRT") ||
                blockName.contains("SAND") ||
                blockName.contains("GRAVEL") ||
                blockName.contains("SNOW") ||
                blockName.contains("CLAY") ||
                blockName.equals("GRASS_BLOCK") ||
                blockName.equals("GRASS_PATH") ||
                blockName.equals("DIRT_PATH") ||
                blockName.equals("PODZOL") ||
                blockName.equals("MYCELIUM") ||
                blockName.equals("SOUL_SOIL") ||
                blockName.equals("SOUL_SAND") ||
                blockName.equals("MUD");
    }

    private static boolean isHoeBlock(String blockName) {
        return blockName.contains("LEAVES") ||
                blockName.contains("WART_BLOCK") ||
                blockName.contains("SCULK") ||
                blockName.equals("HAY_BLOCK") ||
                blockName.equals("TARGET") ||
                blockName.equals("DRIED_KELP_BLOCK") ||
                blockName.equals("SPONGE") ||
                blockName.equals("WET_SPONGE") ||
                blockName.equals("SHROOMLIGHT") ||
                blockName.equals("MOSS_BLOCK") ||
                blockName.equals("MOSS_CARPET");
    }

    private static boolean isShearsBlock(String blockName) {
        return blockName.contains("LEAVES") ||
                blockName.contains("WOOL") ||
                blockName.contains("VINE") ||
                blockName.equals("COBWEB") ||
                blockName.equals("TRIPWIRE") ||
                blockName.equals("FERN") ||
                blockName.equals("GRASS") ||
                blockName.equals("DEAD_BUSH") ||
                blockName.equals("GLOW_LICHEN");
    }

}
