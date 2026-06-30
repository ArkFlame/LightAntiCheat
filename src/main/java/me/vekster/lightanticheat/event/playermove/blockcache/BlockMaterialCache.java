package me.vekster.lightanticheat.event.playermove.blockcache;

import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Optional;

public final class BlockMaterialCache {

    private BlockMaterialCache() {
    }

    public static Optional<Block> findBlockAt(final Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return findBlockAt(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Block blockAtOrNull(final World world, final int x, final int y, final int z) {
        if (!isLoadedOwned(world, x >> 4, z >> 4)) {
            return null;
        }
        return world.getBlockAt(x, y, z);
    }

    public static Optional<Block> findBlockAt(final World world, final int x, final int y, final int z) {
        return Optional.ofNullable(blockAtOrNull(world, x, y, z));
    }

    public static Optional<Block> findRelative(final Block block, final BlockFace face) {
        if (block == null || face == null) {
            return Optional.empty();
        }
        return findRelative(block, face.getModX(), face.getModY(), face.getModZ());
    }

    public static Optional<Block> findRelative(final Block block, final int modX, final int modY, final int modZ) {
        if (block == null) {
            return Optional.empty();
        }
        return findBlockAt(block.getWorld(), block.getX() + modX, block.getY() + modY, block.getZ() + modZ);
    }

    public static Optional<Material> findType(final Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return findType(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Optional<Material> findType(final Block block) {
        if (block == null) {
            return Optional.empty();
        }
        return findType(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public static Material typeAtOrAir(final World world, final int x, final int y, final int z) {
        final Block block = blockAtOrNull(world, x, y, z);
        return block == null ? Material.AIR : block.getType();
    }

    public static Material typeOrAir(final World world, final int x, final int y, final int z) {
        return typeAtOrAir(world, x, y, z);
    }

    public static Optional<Material> findType(final World world, final int x, final int y, final int z) {
        if (!isLoadedOwned(world, x >> 4, z >> 4)) {
            return Optional.empty();
        }
        return Optional.of(world.getBlockAt(x, y, z).getType());
    }

    public static Material typeOrAir(final Block block) {
        return findType(block).orElse(Material.AIR);
    }

    public static Material typeOrAir(final Location location) {
        return findType(location).orElse(Material.AIR);
    }

    public static Material relativeTypeOrAir(final Block block, final BlockFace face) {
        if (block == null || face == null) {
            return Material.AIR;
        }
        return relativeTypeOrAir(block, face.getModX(), face.getModY(), face.getModZ());
    }

    public static Material relativeTypeOrAir(final Block block, final int modX, final int modY, final int modZ) {
        if (block == null) {
            return Material.AIR;
        }
        return typeAtOrAir(block.getWorld(), block.getX() + modX, block.getY() + modY, block.getZ() + modZ);
    }

    public static boolean isLiquid(final Block block) {
        if (!isLoadedOwned(block)) {
            return false;
        }
        return block.isLiquid();
    }

    public static boolean isEmpty(final Block block) {
        if (!isLoadedOwned(block)) {
            return true;
        }
        return block.isEmpty();
    }

    public static boolean isLoadedOwned(final Block block) {
        if (block == null) {
            return false;
        }
        return isLoadedOwned(block.getWorld(), block.getX() >> 4, block.getZ() >> 4);
    }

    public static boolean isLoadedOwned(final Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return isLoadedOwned(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static boolean isLoadedOwned(final World world, final int chunkX, final int chunkZ) {
        if (world == null) {
            return false;
        }
        if (FoliaUtil.isFolia() && !FoliaUtil.isOwnedByCurrentRegion(world, chunkX, chunkZ, 0)) {
            return false;
        }
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    public static void invalidateBlock(final Block block) {
        // Stateless by design: movement reads are not retained globally.
    }

    public static void invalidateChunk(final World world, final int chunkX, final int chunkZ) {
        // Stateless by design: movement reads are not retained globally.
    }

    public static void invalidateWorld(final World world) {
        // Stateless by design: movement reads are not retained globally.
    }

    public static void clear() {
        // Stateless by design: movement reads are not retained globally.
    }

    private static long chunkKey(final int chunkX, final int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static long blockKey(final int x, final int y, final int z) {
        return ((long) (x & 15) << 48) | ((long) (z & 15) << 44) | (y & 0x00000FFFFFFFFFFFL);
    }
}
