package me.vekster.lightanticheat.event.playermove.blockcache;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class BlockMaterialCacheInvalidationListener implements Listener {

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        BlockMaterialCache.invalidateChunk(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler(ignoreCancelled = false)
    public void onBlockPlace(final BlockPlaceEvent event) {
        BlockMaterialCache.invalidateBlock(event.getBlock());
        BlockMaterialCache.invalidateBlock(event.getBlockAgainst());
    }

    @EventHandler(ignoreCancelled = false)
    public void onBlockBreak(final BlockBreakEvent event) {
        BlockMaterialCache.invalidateBlock(event.getBlock());
    }

    @EventHandler
    public void onWorldUnload(final WorldUnloadEvent event) {
        BlockMaterialCache.invalidateWorld(event.getWorld());
    }
}
