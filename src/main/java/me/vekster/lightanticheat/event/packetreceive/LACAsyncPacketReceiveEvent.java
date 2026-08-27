package me.vekster.lightanticheat.event.packetreceive;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.input.model.LACPacketFrame;
import me.vekster.lightanticheat.input.model.LACPacketType;
import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;
import java.util.Set;

public class LACAsyncPacketReceiveEvent extends Event implements LACPlayerContextEvent {
    private static final HandlerList handlers = new HandlerList();
    private final LACPlayer.Context context;
    private final LACPacketType packetType;
    private final int entityId;
    private final Optional<Location> location;
    private final Set<Block> downBlocks;
    private final Set<Material> downMaterials;

    public LACAsyncPacketReceiveEvent(LACPlayer.Context context, LACPacketFrame frame,
                                      Optional<Location> location,
                                      me.vekster.lightanticheat.event.playermove.blockcache.BlockCache blockCache) {
        super(!FoliaUtil.isFolia());
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        this.context = context;
        this.packetType = frame.getPacketType() != null ? frame.getPacketType() : LACPacketType.OTHER;
        this.entityId = frame.getEntityId();
        if (this.packetType == LACPacketType.FLYING && location != null && location.isPresent()) {
            this.location = Optional.of(location.get().clone());
            if (blockCache != null) {
                this.downBlocks = blockCache.downBlocks;
                this.downMaterials = blockCache.downMaterials;
            } else {
                me.vekster.lightanticheat.event.playermove.blockcache.BlockCache empty =
                        me.vekster.lightanticheat.event.playermove.blockcache.BlockCache.empty();
                this.downBlocks = empty.downBlocks;
                this.downMaterials = empty.downMaterials;
            }
        } else {
            this.location = Optional.empty();
            me.vekster.lightanticheat.event.playermove.blockcache.BlockCache empty =
                    me.vekster.lightanticheat.event.playermove.blockcache.BlockCache.empty();
            this.downBlocks = empty.downBlocks;
            this.downMaterials = empty.downMaterials;
        }
    }

    // Backward compat constructor for old typo package delegates (kept for transitional compile if needed)
    public LACAsyncPacketReceiveEvent(LACPlayer.Context context, LACPacketType packetType, int entityId,
                                      Optional<Location> location, Set<Block> downBlocks, Set<Material> downMaterials) {
        super(!FoliaUtil.isFolia());
        this.context = context;
        this.packetType = packetType != null ? packetType : LACPacketType.OTHER;
        this.entityId = entityId;
        this.location = location != null ? location : Optional.empty();
        me.vekster.lightanticheat.event.playermove.blockcache.BlockCache empty =
                me.vekster.lightanticheat.event.playermove.blockcache.BlockCache.empty();
        this.downBlocks = downBlocks != null ? downBlocks : empty.downBlocks;
        this.downMaterials = downMaterials != null ? downMaterials : empty.downMaterials;
    }

    public Player getPlayer() {
        return context.player();
    }

    public LACPlayer getLacPlayer() {
        return context.owner();
    }

    public LACPlayer.Context getContext() {
        return context;
    }

    public LACPacketType getPacketType() {
        return packetType;
    }

    public int getEntityId() {
        return entityId;
    }

    public Optional<Location> getLocation() {
        return location.isPresent() ? Optional.of(location.get().clone()) : Optional.<Location>empty();
    }

    public Set<Block> getDownBlocks() {
        return downBlocks;
    }

    public Set<Material> getDownMaterials() {
        return downMaterials;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
