package me.vekster.lightanticheat.event;

import com.fren_gor.lightInjector.LightInjector;
import io.netty.channel.Channel;
import me.vekster.lightanticheat.Main;
import java.util.Optional;
import me.vekster.lightanticheat.event.bus.LACEventBus;
import me.vekster.lightanticheat.event.bus.LACEventType;
import me.vekster.lightanticheat.event.packetrecive.LACAsyncPacketReceiveEvent;
import me.vekster.lightanticheat.event.packetrecive.packettype.PacketRecognitionResult;
import me.vekster.lightanticheat.event.packetrecive.packettype.PacketType;
import me.vekster.lightanticheat.event.packetrecive.packettype.PacketTypeRecognizer;
import me.vekster.lightanticheat.event.playerattack.LACAsyncPlayerAttackEvent;
import me.vekster.lightanticheat.event.playerattack.LACPlayerAttackEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACAsyncPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playerbreakblock.LACPlayerBreakBlockEvent;
import me.vekster.lightanticheat.event.playermove.LACAsyncPlayerMoveEvent;
import me.vekster.lightanticheat.event.playermove.LACPlayerMoveEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACAsyncPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.event.playerplaceblock.LACPlayerPlaceBlockEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.player.LACPlayerManager;
import me.vekster.lightanticheat.util.config.ConfigManager;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import me.vekster.lightanticheat.version.identifier.LACVersion;
import me.vekster.lightanticheat.version.identifier.VerIdentifier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LACEventCaller extends LightInjector implements Listener {

    public LACEventCaller() {
        super(Main.getInstance());
    }

    public static void callMovementEvents(PlayerMoveEvent event) {
        if (CheckUtil.isExternalNPC(event))
            return;
        if (event.getTo() == null)
            return;
        Player player = event.getPlayer();
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (ctxOpt.isEmpty())
            return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false))
            return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null)
            return;
        if (!from.getWorld().getUID().equals(context.worldId()))
            return;
        if (!to.getWorld().getUID().equals(context.worldId()))
            return;
        LACPlayerMoveEvent sync = new LACPlayerMoveEvent(event, context, from, to);
        LACEventBus.call(LACEventType.PLAYER_MOVE, sync);
        if (!context.isCurrent())
            return;
        LACEventBus.call(LACEventType.ASYNC_PLAYER_MOVE, new LACAsyncPlayerMoveEvent(sync));
    }

    public static void callEntityDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();
        if (CheckUtil.isExternalNPC(player))
            return;
        if (CheckUtil.isExternalNPC(event.getEntity()))
            return;
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (ctxOpt.isEmpty())
            return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false))
            return;
        if (event.getEntity().getWorld() == null || !event.getEntity().getWorld().getUID().equals(context.worldId()))
            return;
        LACEventBus.call(LACEventType.PLAYER_ATTACK, new LACPlayerAttackEvent(event, context, event.getEntity()));
        LACEventBus.call(LACEventType.ASYNC_PLAYER_ATTACK, new LACAsyncPlayerAttackEvent(context, event.getEntity().getEntityId()));
    }

    public static void callBlockPlaceEvents(BlockPlaceEvent event) {
        if (CheckUtil.isExternalNPC(event.getPlayer()))
            return;
        Player player = event.getPlayer();
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (ctxOpt.isEmpty())
            return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false))
            return;
        if (event.getBlock().getWorld() == null || !event.getBlock().getWorld().getUID().equals(context.worldId()))
            return;
        LACPlayerPlaceBlockEvent sync = new LACPlayerPlaceBlockEvent(event, context, event.getBlock(), event.getBlockAgainst(), event.getBlockReplacedState());
        LACEventBus.call(LACEventType.PLAYER_PLACE_BLOCK, sync);
        LACEventBus.call(LACEventType.ASYNC_PLAYER_PLACE_BLOCK, new LACAsyncPlayerPlaceBlockEvent(sync));
    }

    public static void callBlockBreakEvents(BlockBreakEvent event) {
        if (CheckUtil.isExternalNPC(event.getPlayer()))
            return;
        Player player = event.getPlayer();
        Optional<LACPlayer.Context> ctxOpt = LACPlayerManager.current(player);
        if (ctxOpt.isEmpty())
            return;
        LACPlayer.Context context = ctxOpt.get();
        if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), false))
            return;
        if (event.getBlock().getWorld() == null || !event.getBlock().getWorld().getUID().equals(context.worldId()))
            return;
        LACPlayerBreakBlockEvent sync = new LACPlayerBreakBlockEvent(event, context, event.getBlock());
        LACEventBus.call(LACEventType.PLAYER_BREAK_BLOCK, sync);
        LACEventBus.call(LACEventType.ASYNC_PLAYER_BREAK_BLOCK, new LACAsyncPlayerBreakBlockEvent(sync));
    }

    @Override
    protected @Nullable Object onPacketReceiveAsync(@Nullable Player sender, @NotNull Channel channel, @NotNull Object nmsPacket) {
        if (!ConfigManager.Config.enabled) return nmsPacket;
        if (sender == null) return nmsPacket;
        final PacketRecognitionResult recognition = PacketTypeRecognizer.recognize(nmsPacket);
        LACPlayerManager.execute(sender, true, context -> {
            if (CheckUtil.shouldSkipJavaWhenBedrockOnly(context.player(), context.owner(), true)) return;
            final LACAsyncPacketReceiveEvent packetEvent = new LACAsyncPacketReceiveEvent(context, recognition);
            if (packetEvent.getPacketType() == PacketType.USE_ENTITY
                    && VerIdentifier.getVersion().isNewerThan(LACVersion.V1_8)) {
                LACEventBus.call(LACEventType.ASYNC_PLAYER_ATTACK,
                        new LACAsyncPlayerAttackEvent(context, packetEvent.getEntityId()));
            }
            LACEventBus.call(LACEventType.ASYNC_PACKET_RECEIVE, packetEvent);
        });
        return nmsPacket;
    }

    @Override
    protected @Nullable Object onPacketSendAsync(@Nullable Player receiver, @NotNull Channel channel, @NotNull Object nmsPacket) {
        return nmsPacket;
    }

}
