package me.vekster.lightanticheat.event.playermove.blockcache;

import me.vekster.lightanticheat.player.LACPlayer;
import me.vekster.lightanticheat.util.detection.CheckUtil;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class BlockCache {

    private static final BlockCache EMPTY = new BlockCache(true);

    public final UUID worldId;
    public final int chunkX;
    public final int chunkZ;
    private final boolean readable;

    private final double sampleX;
    private final double sampleY;
    private final double sampleZ;

    public final Set<Block> withinBlocks;
    public final Set<Material> withinMaterials;
    public final boolean withinBlocksPassable;
    public final Set<Block> downBlocks;
    public final Set<Material> downMaterials;
    public final boolean downBlocksPassable;
    public final Set<Block> interactiveBlocks;
    public final Set<Material> interactiveMaterials;

    public final boolean playerClimbing;
    public final boolean playerInWater;

    private BlockCache(boolean emptyMarker) {
        this.worldId = null;
        this.chunkX = 0;
        this.chunkZ = 0;
        this.readable = false;
        this.sampleX = 0;
        this.sampleY = 0;
        this.sampleZ = 0;
        this.withinBlocks = Collections.emptySet();
        this.withinMaterials = EnumSet.noneOf(Material.class);
        this.withinBlocksPassable = true;
        this.downBlocks = Collections.emptySet();
        this.downMaterials = EnumSet.noneOf(Material.class);
        this.downBlocksPassable = true;
        this.interactiveBlocks = Collections.emptySet();
        this.interactiveMaterials = EnumSet.noneOf(Material.class);
        this.playerClimbing = false;
        this.playerInWater = false;
    }

    private BlockCache(final UUID worldId, final int chunkX, final int chunkZ,
            final double sampleX, final double sampleY, final double sampleZ, final boolean readable,
            final Set<Block> withinBlocks, final Set<Material> withinMaterials, final boolean withinBlocksPassable,
            final Set<Block> downBlocks, final Set<Material> downMaterials, final boolean downBlocksPassable,
            final Set<Block> interactiveBlocks, final Set<Material> interactiveMaterials,
            final boolean playerClimbing, final boolean playerInWater) {
        this.worldId = worldId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.readable = readable;
        this.sampleX = sampleX;
        this.sampleY = sampleY;
        this.sampleZ = sampleZ;
        this.withinBlocks = withinBlocks;
        this.withinMaterials = withinMaterials;
        this.withinBlocksPassable = withinBlocksPassable;
        this.downBlocks = downBlocks;
        this.downMaterials = downMaterials;
        this.downBlocksPassable = downBlocksPassable;
        this.interactiveBlocks = interactiveBlocks;
        this.interactiveMaterials = interactiveMaterials;
        this.playerClimbing = playerClimbing;
        this.playerInWater = playerInWater;
    }

    public static BlockCache capture(final LACPlayer.Context context, final Location location) {
        if (context == null || location == null || location.getWorld() == null) {
            return empty();
        }
        if (!context.isCurrent()) {
            return empty();
        }
        if (!location.getWorld().getUID().equals(context.worldId())) {
            return empty();
        }
        if (FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(context.player())) {
            return empty();
        }
        if (FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(location, 1)) {
            return empty();
        }

        final int chunkX = location.getBlockX() >> 4;
        final int chunkZ = location.getBlockZ() >> 4;
        if (!BlockMaterialCache.isLoadedOwned(location.getWorld(), chunkX, chunkZ)) {
            return empty();
        }

        final UUID worldId = location.getWorld().getUID();
        final double sampleX = location.getX();
        final double sampleY = location.getY();
        final double sampleZ = location.getZ();
        final boolean readable = true;

        final Set<Block> withinBlocks = CheckUtil.getWithinBlocks(context.player(), location);
        final Set<Material> withinMaterials = EnumSet.noneOf(Material.class);
        boolean withinBlocksPassable = true;
        for (final Block block : withinBlocks) {
            withinMaterials.add(BlockMaterialCache.typeOrAir(block));
            if (withinBlocksPassable && !CheckUtil.isActuallyPassable(block)) {
                withinBlocksPassable = false;
            }
        }

        final Set<Block> downBlocks = CheckUtil.getDownBlocks(context.player(), location, 0.21);
        final Set<Material> downMaterials = EnumSet.noneOf(Material.class);
        boolean downBlocksPassable = true;
        for (final Block block : downBlocks) {
            downMaterials.add(BlockMaterialCache.typeOrAir(block));
            if (downBlocksPassable && !CheckUtil.isActuallyPassable(block)) {
                downBlocksPassable = false;
            }
        }

        final Set<Block> interactiveBlocks = CheckUtil.getInteractiveBlocks(context.player(), location);
        final Set<Material> interactiveMaterials = EnumSet.noneOf(Material.class);
        for (final Block block : interactiveBlocks) {
            interactiveMaterials.add(BlockMaterialCache.typeOrAir(block));
        }

        final boolean playerClimbing = resolveClimbing(withinBlocks, downBlocks, interactiveBlocks);
        final boolean playerInWater = resolveWater(downBlocks, withinBlocks, interactiveBlocks);

        if (!context.isCurrent()) {
            return empty();
        }
        if (!location.getWorld().getUID().equals(context.worldId())) {
            return empty();
        }
        if (FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(context.player())) {
            return empty();
        }
        if (FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(location, 1)) {
            return empty();
        }

        return new BlockCache(worldId, chunkX, chunkZ, sampleX, sampleY, sampleZ, readable,
                withinBlocks, withinMaterials, withinBlocksPassable,
                downBlocks, downMaterials, downBlocksPassable,
                interactiveBlocks, interactiveMaterials, playerClimbing, playerInWater);
    }

    public static BlockCache empty() {
        return EMPTY;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean matches(final Location location) {
        if (!readable) {
            return false;
        }
        if (location == null || location.getWorld() == null || worldId == null) {
            return false;
        }
        if (!worldId.equals(location.getWorld().getUID())) {
            return false;
        }
        return Double.doubleToLongBits(sampleX) == Double.doubleToLongBits(location.getX())
                && Double.doubleToLongBits(sampleY) == Double.doubleToLongBits(location.getY())
                && Double.doubleToLongBits(sampleZ) == Double.doubleToLongBits(location.getZ());
    }

    private static boolean isClimbingMaterial(final Material material) {
        if (material == null) {
            return false;
        }
        final String name = material.name();
        return name.equals("LADDER") || name.equals("SCAFFOLDING") || name.contains("VINE");
    }

    private static boolean containsClimbingMaterial(final Set<Material> materials) {
        for (final Material material : materials) {
            if (isClimbingMaterial(material)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Material> materialsOf(final Set<Block> blocks) {
        final Set<Material> materials = EnumSet.noneOf(Material.class);
        for (final Block block : blocks) {
            materials.add(BlockMaterialCache.typeOrAir(block));
        }
        return materials;
    }

    private static boolean containsWaterLike(final Set<Block> blocks) {
        for (final Block block : blocks) {
            if (BlockMaterialCache.isWaterLike(block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean resolveClimbing(final Set<Block> withinBlocks, final Set<Block> downBlocks,
            final Set<Block> interactiveBlocks) {
        if (containsClimbingMaterial(materialsOf(withinBlocks))) {
            return true;
        }
        if (containsClimbingMaterial(materialsOf(downBlocks))) {
            return true;
        }
        if (containsClimbingMaterial(materialsOf(interactiveBlocks))) {
            return true;
        }
        return false;
    }

    private static boolean resolveWater(final Set<Block> downBlocks, final Set<Block> withinBlocks,
            final Set<Block> interactiveBlocks) {
        if (containsWaterLike(downBlocks)) {
            return true;
        }
        if (containsWaterLike(withinBlocks)) {
            return true;
        }
        if (containsWaterLike(interactiveBlocks)) {
            return true;
        }
        return false;
    }
}
