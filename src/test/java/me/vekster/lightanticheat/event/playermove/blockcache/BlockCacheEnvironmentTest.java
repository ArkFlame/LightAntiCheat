package me.vekster.lightanticheat.event.playermove.blockcache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import me.vekster.lightanticheat.event.playermove.blockcache.BlockMaterialCache;
import org.bukkit.Material;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class BlockCacheEnvironmentTest {

    private static boolean isClimbingMaterial(Material m) throws Exception {
        Method mtd = Class.forName("me.vekster.lightanticheat.event.playermove.blockcache.BlockCache")
                .getDeclaredMethod("isClimbingMaterial", Material.class);
        mtd.setAccessible(true);
        return (boolean) mtd.invoke(null, m);
    }

    @Test
    void ladderIsClimbing() throws Exception {
        Material m = Material.getMaterial("LADDER");
        Assumptions.assumeTrue(m != null);
        assertTrue(isClimbingMaterial(m));
    }

    @Test
    void vineIsClimbing() throws Exception {
        Material m = Material.getMaterial("VINE");
        Assumptions.assumeTrue(m != null);
        assertTrue(isClimbingMaterial(m));
    }

    @Test
    void scaffoldingWhenPresentIsClimbing() throws Exception {
        Material m = Material.getMaterial("SCAFFOLDING");
        Assumptions.assumeTrue(m != null);
        assertTrue(isClimbingMaterial(m));
    }

    @Test
    void trapdoorAloneIsNotClimbing() throws Exception {
        Material m = Material.getMaterial("TRAPDOOR");
        Assumptions.assumeTrue(m != null);
        assertFalse(isClimbingMaterial(m));
    }

    @Test
    void waterIsWaterMaterial() {
        Material m = Material.getMaterial("WATER");
        Assumptions.assumeTrue(m != null);
        assertTrue(BlockMaterialCache.isWaterMaterial(m));
    }

    @Test
    void stationaryWaterWhenPresentIsWaterMaterial() {
        Material m = Material.getMaterial("STATIONARY_WATER");
        Assumptions.assumeTrue(m != null);
        assertTrue(BlockMaterialCache.isWaterMaterial(m));
    }

    @Test
    void bubbleColumnWhenPresentIsWaterMaterial() {
        Material m = Material.getMaterial("BUBBLE_COLUMN");
        Assumptions.assumeTrue(m != null);
        assertTrue(BlockMaterialCache.isWaterMaterial(m));
    }

    @Test
    void stoneIsNotWaterMaterial() {
        Material m = Material.getMaterial("STONE");
        Assumptions.assumeTrue(m != null);
        assertFalse(BlockMaterialCache.isWaterMaterial(m));
    }
}
