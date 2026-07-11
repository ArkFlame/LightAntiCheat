package me.vekster.lightanticheat.event.playerbreakblock;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class LACPlayerBreakBlockEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private BlockBreakEvent event;
    private final LACPlayer.Context context;
    private Block block;

    public LACPlayerBreakBlockEvent(BlockBreakEvent event, LACPlayer.Context context, Block block) {
        this.event = event;
        this.context = context;
        this.block = block;
    }

    public BlockBreakEvent getEvent() {
        return event;
    }

    public LACPlayer.Context getContext() {
        return context;
    }

    public Player getPlayer() {
        return context.player();
    }

    public LACPlayer getLacPlayer() {
        return context.owner();
    }

    public Block getBlock() {
        return block;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
