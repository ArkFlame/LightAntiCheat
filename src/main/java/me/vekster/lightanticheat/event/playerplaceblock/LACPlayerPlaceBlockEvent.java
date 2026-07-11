package me.vekster.lightanticheat.event.playerplaceblock;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockPlaceEvent;

public class LACPlayerPlaceBlockEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private BlockPlaceEvent event;
    private final LACPlayer.Context context;
    private Block block;
    private Block blockAgainst;
    private BlockState blockReplacedState;

    public LACPlayerPlaceBlockEvent(BlockPlaceEvent event, LACPlayer.Context context,
                                    Block block, Block blockAgainst, BlockState blockReplacedState) {
        this.event = event;
        this.context = context;
        this.block = block;
        this.blockAgainst = blockAgainst;
        this.blockReplacedState = blockReplacedState;
    }

    public BlockPlaceEvent getEvent() {
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

    public Block getBlockAgainst() {
        return blockAgainst;
    }

    public BlockState getBlockReplacedState() {
        return blockReplacedState;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
