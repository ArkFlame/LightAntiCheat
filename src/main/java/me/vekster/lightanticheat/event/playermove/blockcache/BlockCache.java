package me.vekster.lightanticheat.event.playermove.blockcache;

import me.vekster.lightanticheat.util.detection.CheckUtil;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BlockCache {

    private static final BlockCache EMPTY = new BlockCache(true);

    public final UUID worldId;
    public final int chunkX;
    public final int chunkZ;
    private final boolean readable;

    public final Set<Block> withinBlocks;
    public final Set<Material> withinMaterials;
    public final boolean withinBlocksPassable;
    public final Set<Block> downBlocks;
    public final Set<Material> downMaterials;
    public final boolean downBlocksPassable;
    public final Set<Block> interactiveBlocks;
    public final Set<Material> interactiveMaterials;

    private BlockCache(boolean emptyMarker) {
        this.worldId = null;
        this.chunkX = 0;
        this.chunkZ = 0;
        this.readable = false;
        this.withinBlocks = Collections.emptySet();
        this.withinMaterials = EnumSet.noneOf(Material.class);
        this.withinBlocksPassable = true;
        this.downBlocks = Collections.emptySet();
        this.downMaterials = EnumSet.noneOf(Material.class);
        this.downBlocksPassable = true;
        this.interactiveBlocks = Collections.emptySet();
        this.interactiveMaterials = EnumSet.noneOf(Material.class);
    }

    public BlockCache(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            this.worldId = null;
            this.chunkX = 0;
            this.chunkZ = 0;
            this.readable = false;
            this.withinBlocks = Collections.emptySet();
            this.withinMaterials = EnumSet.noneOf(Material.class);
            this.withinBlocksPassable = true;
            this.downBlocks = Collections.emptySet();
            this.downMaterials = EnumSet.noneOf(Material.class);
            this.downBlocksPassable = true;
            this.interactiveBlocks = Collections.emptySet();
            this.interactiveMaterials = EnumSet.noneOf(Material.class);
            return;
        }

        World world = location.getWorld();
        this.worldId = world.getUID();
        this.chunkX = location.getBlockX() >> 4;
        this.chunkZ = location.getBlockZ() >> 4;

        if ((FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(location, 1))
                || !BlockMaterialCache.isLoadedOwned(world, this.chunkX, this.chunkZ)) {
            this.readable = false;
            this.withinBlocks = Collections.emptySet();
            this.withinMaterials = EnumSet.noneOf(Material.class);
            this.withinBlocksPassable = true;
            this.downBlocks = Collections.emptySet();
            this.downMaterials = EnumSet.noneOf(Material.class);
            this.downBlocksPassable = true;
            this.interactiveBlocks = Collections.emptySet();
            this.interactiveMaterials = EnumSet.noneOf(Material.class);
            return;
        }

        this.readable = true;

        this.withinBlocks = CheckUtil.getWithinBlocks(player, location);
        this.withinMaterials = EnumSet.noneOf(Material.class);
        boolean toWithinBlocksPassable = true;
        for (Block block : this.withinBlocks) {
            this.withinMaterials.add(BlockMaterialCache.typeOrAir(block));
            if (toWithinBlocksPassable && !CheckUtil.isActuallyPassable(block))
                toWithinBlocksPassable = false;
        }
        this.withinBlocksPassable = toWithinBlocksPassable;

        this.downBlocks = CheckUtil.getDownBlocks(player, location, 0.21);
        this.downMaterials = EnumSet.noneOf(Material.class);
        boolean toDownBlocksPassable = true;
        for (Block block : this.downBlocks) {
            this.downMaterials.add(BlockMaterialCache.typeOrAir(block));
            if (toDownBlocksPassable && !CheckUtil.isActuallyPassable(block))
                toDownBlocksPassable = false;
        }
        this.downBlocksPassable = toDownBlocksPassable;

        this.interactiveBlocks = CheckUtil.getInteractiveBlocks(player, location);
        this.interactiveMaterials = EnumSet.noneOf(Material.class);
        for (Block block : this.interactiveBlocks) {
            this.interactiveMaterials.add(BlockMaterialCache.typeOrAir(block));
        }
    }

    public static BlockCache empty() {
        return EMPTY;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean matchesWorld(final Location location) {
        return location != null && location.getWorld() != null && worldId != null
                && worldId.equals(location.getWorld().getUID());
    }
}
