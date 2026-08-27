package me.vekster.lightanticheat.input;

import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.playerattack.LACAsyncPlayerAttackEvent;
import me.vekster.lightanticheat.event.playerattack.LACPlayerAttackEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACAsyncPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACPlayerMoveEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.input.model.LACInputMode;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public final class LACBukkitStateBridge {

    private final LACInputEngine engine;

    public LACBukkitStateBridge(LACInputEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine must not be null");
        }
        this.engine = engine;
    }

    public void onMovement(PlayerMoveEvent event) {
        if (event == null) return;
        if (CheckUtil.isExternalNPC(event)) return;
        if (event.getTo() == null) return;
        Player player = event.getPlayer();
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (!ctxOpt.isPresent()) return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false)) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (!from.getWorld().getUID().equals(context.worldId())) return;
        if (!to.getWorld().getUID().equals(context.worldId())) return;

        LACPlayerMoveEvent sync = new LACPlayerMoveEvent(event, context, from, to);
        LACEventBus.call(LACEventType.PLAYER_MOVE, sync);

        if (!context.isCurrent()) return;

        LACInputMode mode = engine.getActiveMode();
        if (mode == LACInputMode.NMS) {
            LACEventBus.call(LACEventType.ASYNC_PLAYER_MOVE, new LACAsyncPlayerMoveEvent(sync));
        }
        // PACKET mode: async movement is emitted from packet queue via dispatcher, not here
    }

    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event == null) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (CheckUtil.isExternalNPC(player)) return;
        if (CheckUtil.isExternalNPC(event.getEntity())) return;
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (!ctxOpt.isPresent()) return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false)) return;
        if (event.getEntity().getWorld() == null || !event.getEntity().getWorld().getUID().equals(context.worldId())) return;

        LACEventBus.call(LACEventType.PLAYER_ATTACK, new LACPlayerAttackEvent(event, context, event.getEntity()));
        // Preserve server-confirmed async derivative; do not duplicate USE_ENTITY provider logic
        LACEventBus.call(LACEventType.ASYNC_PLAYER_ATTACK, new LACAsyncPlayerAttackEvent(context, event.getEntity().getEntityId()));
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (event == null) return;
        if (CheckUtil.isExternalNPC(event.getPlayer())) return;
        Player player = event.getPlayer();
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (!ctxOpt.isPresent()) return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false)) return;
        if (event.getBlock().getWorld() == null || !event.getBlock().getWorld().getUID().equals(context.worldId())) return;
        LACPlayerPlaceBlockEvent sync = new LACPlayerPlaceBlockEvent(event, context, event.getBlock(), event.getBlockAgainst(), event.getBlockReplacedState());
        LACEventBus.call(LACEventType.PLAYER_PLACE_BLOCK, sync);
        LACEventBus.call(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, new LACAsyncPlayerPlaceBlockEvent(sync));
    }

    public void onBlockBreak(BlockBreakEvent event) {
        if (event == null) return;
        if (CheckUtil.isExternalNPC(event.getPlayer())) return;
        Player player = event.getPlayer();
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (!ctxOpt.isPresent()) return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false)) return;
        if (event.getBlock().getWorld() == null || !event.getBlock().getWorld().getUID().equals(context.worldId())) return;
        LACPlayerBreakBlockEvent sync = new LACPlayerBreakBlockEvent(event, context, event.getBlock());
        LACEventBus.call(LACEventType.PLAYER_BREAK_BLOCK, sync);
        LACEventBus.call(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, new LACAsyncPlayerBreakBlockEvent(sync));
    }
}
