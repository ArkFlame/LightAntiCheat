package me.vekster.lightanticheat.util.detection.specific;

import me.vekster.lightanticheat.event.playermove.blockcache.BlockMaterialCache;
import me.vekster.lightanticheat.util.async.AsyncUtil;
import me.vekster.lightanticheat.util.hook.server.folia.FoliaUtil;
import me.vekster.lightanticheat.util.scheduler.Scheduler;
import me.vekster.lightanticheat.version.VerUtil;
import me.vekster.lightanticheat.version.identifier.LACVersion;
import me.vekster.lightanticheat.version.identifier.VerIdentifier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockUtil {

    public static Set<Block> getWithinBlocksClearly(Entity player, Location location) {
        World world = AsyncUtil.getWorld(player);
        if (world == null) return Collections.emptySet();
        double halfWidth = VerUtil.getWidth(player) / 2.0;
        double height = VerUtil.getHeight(player);
        double startX = location.getX() - halfWidth;
        double startY = location.getY();
        double startZ = location.getZ() - halfWidth;
        double endX = location.getX() + halfWidth;
        double endY = location.getY() + height;
        double endZ = location.getZ() + halfWidth;
        int minX = (int) Math.floor(startX);
        int minY = (int) Math.floor(startY);
        int minZ = (int) Math.floor(startZ);
        int maxX = (int) Math.floor(endX);
        if (endX % 1.0 == 0) maxX--;
        int maxY = (int) Math.floor(endY);
        if (endY % 1.0 == 0) maxY--;
        int maxZ = (int) Math.floor(endZ);
        if (endZ % 1.0 == 0) maxZ--;
        Set<Block> blocks = new HashSet<>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockMaterialCache.findBlockAt(world, x, y, z).ifPresent(blocks::add);
                }
            }
        }
        return blocks;
    }

    public static Set<Block> getWithinBlocks(Entity player, Location location) {
        if (VerIdentifier.getVersion().isNewerThan(LACVersion.V1_8) || FoliaUtil.isFolia() || Bukkit.isPrimaryThread())
            return getWithinBlocksClearly(player, location);
        CompletableFuture<Set<Block>> result = new CompletableFuture<>();
        Scheduler.runTask(true, () -> {
            result.complete(getWithinBlocksClearly(player, location));
        });
        try {
            return result.get(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return getWithinBlocksClearly(player, location);
        }
    }

    public static Set<Block> getWithinBlocks(Entity player) {
        return getWithinBlocks(player, player.getLocation());
    }

    private static Set<Block> getInteractiveBlocksClearly(Entity player, Location location, boolean removeCorners) {
        World world = AsyncUtil.getWorld(player);
        if (world == null) return Collections.emptySet();
        double halfWidth = VerUtil.getWidth(player) / 2.0;
        double height = VerUtil.getHeight(player);
        double startX = location.getX() - halfWidth;
        double startY = location.getY();
        double startZ = location.getZ() - halfWidth;
        double endX = location.getX() + halfWidth;
        double endY = location.getY() + height;
        double endZ = location.getZ() + halfWidth;
        int minX = (int) Math.floor(startX) - 1;
        int minY = (int) Math.floor(startY);
        int minZ = (int) Math.floor(startZ) - 1;
        int maxX = (int) Math.floor(endX);
        if (endX % 1.0 == 0) maxX--;
        maxX++;
        int maxZ = (int) Math.floor(endZ);
        if (endZ % 1.0 == 0) maxZ--;
        maxZ++;
        Set<Block> blocks = new HashSet<>((maxX - minX + 1) * (maxZ - minZ + 1) * 2);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (removeCorners && (z == minZ || z == maxZ) && (x == minX || x == maxX))
                    continue;
                Block block = AsyncUtil.getBlockAt(world, x, minY, z);
                if (block != null) blocks.add(block);
                if (location.getY() % 1.0 != 0) {
                    block = AsyncUtil.getBlockAt(world, x, minY + 1, z);
                    if (block != null) blocks.add(block);
                }
            }
        }
        return blocks;
    }

    private static Set<Block> getInteractiveBlocks(Entity player, Location location, boolean removeCorners) {
        if (VerIdentifier.getVersion().isNewerThan(LACVersion.V1_8) || FoliaUtil.isFolia() || Bukkit.isPrimaryThread())
            return getInteractiveBlocksClearly(player, location, removeCorners);
        CompletableFuture<Set<Block>> result = new CompletableFuture<>();
        Scheduler.runTask(true, () -> {
            result.complete(getInteractiveBlocksClearly(player, location, removeCorners));
        });
        try {
            return result.get(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return getInteractiveBlocksClearly(player, location, removeCorners);
        }
    }

    public static Set<Block> getInteractiveBlocks(Entity player, Location location) {
        return getInteractiveBlocks(player, location, true);
    }

    public static Set<Block> getInteractiveBlocks(Entity player) {
        return getInteractiveBlocks(player, player.getLocation());
    }

    public static Set<Block> getCollisionBlockLayer(Entity player, Location location) {
        return getInteractiveBlocks(player, location, false);
    }

    public static Set<Block> getCollisionBlockLayer(Entity player) {
        return getInteractiveBlocks(player, player.getLocation(), false);
    }

    public static Set<Block> getDownBlocksClearly(Entity player, Location location, double padding) {
        World world = AsyncUtil.getWorld(player);
        if (world == null) return Collections.emptySet();
        double halfWidth = VerUtil.getWidth(player) / 2.0 + padding;
        double startX = location.getX() - halfWidth;
        double startY = location.getY();
        double startZ = location.getZ() - halfWidth;
        double endX = location.getX() + halfWidth;
        double endZ = location.getZ() + halfWidth;
        int minX = (int) Math.floor(startX);
        int minY = (int) Math.floor(startY);
        int minZ = (int) Math.floor(startZ);
        int maxX = (int) Math.floor(endX);
        if (endX % 1.0 == 0) maxX--;
        int maxZ = (int) Math.floor(endZ);
        if (endZ % 1.0 == 0) maxZ--;
        Set<Block> blocks = new HashSet<>((maxX - minX + 1) * (maxZ - minZ + 1));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = AsyncUtil.getBlockAt(world, x, minY - (location.getY() % 1 == 0 ? 1 : 0), z);
                if (block != null) blocks.add(block);
            }
        }
        return blocks;
    }

    public static Set<Block> getDownBlocks(Entity player, Location location, double padding) {
        if (VerIdentifier.getVersion().isNewerThan(LACVersion.V1_8) || FoliaUtil.isFolia() || Bukkit.isPrimaryThread())
            return getDownBlocksClearly(player, location, padding);
        CompletableFuture<Set<Block>> result = new CompletableFuture<>();
        Scheduler.runTask(true, () -> {
            result.complete(getDownBlocksClearly(player, location, padding));
        });
        try {
            return result.get(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return getDownBlocksClearly(player, location, padding);
        }
    }

    public static Set<Block> getDownBlocks(Entity player, double padding) {
        return getDownBlocks(player, player.getLocation(), padding);
    }

}
