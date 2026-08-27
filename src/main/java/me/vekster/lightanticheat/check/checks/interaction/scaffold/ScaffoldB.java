package me.vekster.lightanticheat.check.checks.interaction.scaffold;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.check.buffer.Buffer;
import me.vekster.lightanticheat.check.checks.interaction.InteractionCheck;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventPriority;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.playermove.blockcache.BlockMaterialCache;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.cache.PlayerCache;
import me.vekster.lightanticheat.player.cache.history.HistoryElement;
import me.vekster.lightanticheat.util.hook.plugin.FloodgateHook;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

/**
 * Sprint
 */
public class ScaffoldB extends InteractionCheck implements Listener {
    public ScaffoldB() {
        super(CheckName.SCAFFOLD_B);
    }

    @Override
    public void registerLACEvents() {
        LACEventBus.register(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, LACEventPriority.NORMAL, this, "onAsyncBlockPlace", event -> onAsyncBlockPlace((LACAsyncPlayerPlaceBlockEvent) event));
    }

    public void onAsyncBlockPlace(LACAsyncPlayerPlaceBlockEvent event) {
        Player player = event.getPlayer();
        LACPlayer lacPlayer = event.getLacPlayer();
        PlayerCache cache = lacPlayer.cache;

        if (!isCheckAllowed(player, lacPlayer, true))
            return;

        if (!isScaffoldPlacement(player, event.getBlock(), event.getBlockAgainst()))
            return;

        if (FloodgateHook.isBedrockPlayer(player, true))
            return;

        for (Block withinBlock : getWithinBlocks(player)) {
            if (BlockMaterialCache.typeOrAir(withinBlock) != Material.AIR)
                return;
        }

        if (getEffectAmplifier(player, VerUtil.potions.get("LEVITATION")) > 0 ||
                getEffectAmplifier(player, PotionEffectType.SPEED) > 5)
            return;

        for (int i = 0; i < 3 && i < HistoryElement.count(); i++) {
            final HistoryElement element = HistoryElement.at(i);
            if (!cache.history.onEvent.onGround.get(element).towardsFalse ||
                    !cache.history.onPacket.onGround.get(element).towardsFalse)
                return;
        }

        if (!player.isSprinting())
            return;

        Buffer buffer = getBuffer(player, true);
        buffer.put("flags", buffer.getInt("flags") + 1);
        if (buffer.getInt("flags") <= 2)
            return;

        Scheduler.runTask(true, () -> {
            callViolationEvent(player, lacPlayer, null);
        });
    }

}
