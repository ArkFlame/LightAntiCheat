package me.vekster.lightanticheat.event.playerplaceblock;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LACAsyncPlayerPlaceBlockEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private final LACPlayer.Context context;
    private Block block;
    private Block blockAgainst;
    private BlockState blockReplacedState;
    private Location location;
    private Location eyeLocation;

    public LACAsyncPlayerPlaceBlockEvent(LACPlayerPlaceBlockEvent event) {
        super(!FoliaUtil.isFolia());

        this.context = event.getContext();
        this.block = event.getBlock();
        this.blockAgainst = event.getBlockAgainst();
        this.blockReplacedState = event.getBlockReplacedState();
        this.location = event.getContext().player().getLocation().clone();
        this.eyeLocation = event.getContext().player().getLocation().clone();
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

    public Location getLocation() {
        return location.clone();
    }

    public Location getEyeLocation() {
        return eyeLocation.clone();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
