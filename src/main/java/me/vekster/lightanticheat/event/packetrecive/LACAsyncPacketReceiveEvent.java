package me.vekster.lightanticheat.event.packetrecive;

import me.vekster.lightanticheat.event.context.LACPlayerContextEvent;
import me.vekster.lightanticheat.event.packetrecive.packettype.PacketRecognitionResult;
import me.vekster.lightanticheat.event.packetrecive.packettype.PacketType;
import me.vekster.lightanticheat.event.playermove.blockcache.BlockCache;
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
    private final PacketType packetType;
    private final int entityId;
    private final Optional<Location> location;
    private final Set<Block> downBlocks;
    private final Set<Material> downMaterials;

    public LACAsyncPacketReceiveEvent(LACPlayer.Context context, PacketRecognitionResult recognition) {
        super(!FoliaUtil.isFolia());

        this.context = context;
        this.packetType = recognition.getPacketType();
        this.entityId = recognition.getEntityId();
        if (packetType == PacketType.FLYING) {
            final Location location = context.player().getLocation().clone();
            final BlockCache blockCache = BlockCache.capture(context, location);
            this.location = Optional.of(location);
            this.downBlocks = blockCache.downBlocks;
            this.downMaterials = blockCache.downMaterials;
        } else {
            this.location = Optional.empty();
            final BlockCache empty = BlockCache.empty();
            this.downBlocks = empty.downBlocks;
            this.downMaterials = empty.downMaterials;
        }
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

    public PacketType getPacketType() {
        return packetType;
    }

    public int getEntityId() {
        return entityId;
    }

    public Optional<Location> getLocation() {
        return location;
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
