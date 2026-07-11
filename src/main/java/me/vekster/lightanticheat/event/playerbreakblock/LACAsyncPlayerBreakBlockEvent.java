package me.vekster.lightanticheat.event.playerbreakblock;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LACAsyncPlayerBreakBlockEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private final LACPlayer.Context context;
    private Block block;
    private Location location;
    private Location eyeLocation;

    public LACAsyncPlayerBreakBlockEvent(LACPlayerBreakBlockEvent event) {
        super(!FoliaUtil.isFolia());

        this.context = event.getContext();
        this.block = event.getBlock();
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
