package me.vekster.lightanticheat.event.playermove.blockcache;

import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BlockMaterialCache {

    private static final ConcurrentMap<UUID, ConcurrentMap<Long, ConcurrentMap<Long, Material>>> BY_WORLD = new ConcurrentHashMap<>();

    private BlockMaterialCache() {
    }

    public static Optional<Block> findBlockAt(final Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return findBlockAt(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Optional<Block> findBlockAt(final World world, final int x, final int y, final int z) {
        if (!isLoadedOwned(world, x >> 4, z >> 4)) {
            return Optional.empty();
        }
        return Optional.of(world.getBlockAt(x, y, z));
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

    public static Optional<Material> findType(final World world, final int x, final int y, final int z) {
        if (!isLoadedOwned(world, x >> 4, z >> 4)) {
            return Optional.empty();
        }
        final UUID worldId = world.getUID();
        final long chunkKey = chunkKey(x >> 4, z >> 4);
        final long blockKey = blockKey(x, y, z);
        final ConcurrentMap<Long, ConcurrentMap<Long, Material>> worldMap = BY_WORLD.computeIfAbsent(worldId, ignored -> new ConcurrentHashMap<>());
        final ConcurrentMap<Long, Material> chunkMap = worldMap.computeIfAbsent(chunkKey, ignored -> new ConcurrentHashMap<>());
        return Optional.ofNullable(chunkMap.computeIfAbsent(blockKey, ignored -> world.getBlockAt(x, y, z).getType()));
    }

    public static Material typeOrAir(final Block block) {
        return findType(block).orElse(Material.AIR);
    }

    public static Material typeOrAir(final Location location) {
        return findType(location).orElse(Material.AIR);
    }

    public static Material relativeTypeOrAir(final Block block, final BlockFace face) {
        return findRelative(block, face).flatMap(BlockMaterialCache::findType).orElse(Material.AIR);
    }

    public static Material relativeTypeOrAir(final Block block, final int modX, final int modY, final int modZ) {
        return findRelative(block, modX, modY, modZ).flatMap(BlockMaterialCache::findType).orElse(Material.AIR);
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
        if (block == null) {
            return;
        }
        final ConcurrentMap<Long, ConcurrentMap<Long, Material>> worldMap = BY_WORLD.get(block.getWorld().getUID());
        if (worldMap == null) {
            return;
        }
        final long chunkKey = chunkKey(block.getX() >> 4, block.getZ() >> 4);
        final ConcurrentMap<Long, Material> chunkMap = worldMap.get(chunkKey);
        if (chunkMap == null) {
            return;
        }
        chunkMap.remove(blockKey(block.getX(), block.getY(), block.getZ()));
        if (chunkMap.isEmpty()) {
            worldMap.remove(chunkKey, chunkMap);
        }
        if (worldMap.isEmpty()) {
            BY_WORLD.remove(block.getWorld().getUID(), worldMap);
        }
    }

    public static void invalidateChunk(final World world, final int chunkX, final int chunkZ) {
        if (world == null) {
            return;
        }
        final ConcurrentMap<Long, ConcurrentMap<Long, Material>> worldMap = BY_WORLD.get(world.getUID());
        if (worldMap == null) {
            return;
        }
        worldMap.remove(chunkKey(chunkX, chunkZ));
        if (worldMap.isEmpty()) {
            BY_WORLD.remove(world.getUID(), worldMap);
        }
    }

    public static void invalidateWorld(final World world) {
        if (world == null) {
            return;
        }
        BY_WORLD.remove(world.getUID());
    }

    public static void clear() {
        BY_WORLD.clear();
    }

    private static long chunkKey(final int chunkX, final int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private static long blockKey(final int x, final int y, final int z) {
        return ((long) (x & 15) << 48) | ((long) (z & 15) << 44) | (y & 0x00000FFFFFFFFFFFL);
    }
}
