package me.vekster.lightanticheat.util.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verification test. The JSON golden fixtures are stored under
 * src/test/resources/lac-quality-extract and loaded through the
 * test classpath. They were mechanically reconstructed from the
 * surviving generated primitive tables and are frozen regression
 * inputs.
 */
public class VanillaVerticalPhysicsTest {

    private static final String FIXTURE_ROOT = "/lac-quality-extract/";
    private static final double EPS = 1e-12;

    private static Map<Integer, Double> jMap0, jMap1, jMap2, jMap3, jMap4, jMap5, jMap6;
    private static Map<Integer, Double> sMap0, sMap1, sMap2, sMap3, sMap4, sMap5, sMap6;
    private static Map<Integer, Double> heightLimits;
    private static Map<Integer, Double> boatSpeeds;

    @BeforeAll
    public static void loadFixtures() throws IOException {
        String flightA = readResource("flight-a.json");
        jMap0 = loadMap(flightA, "JUMP_0");
        jMap1 = loadMap(flightA, "JUMP_1");
        jMap2 = loadMap(flightA, "JUMP_2");
        jMap3 = loadMap(flightA, "JUMP_3");
        jMap4 = loadMap(flightA, "JUMP_4");
        jMap5 = loadMap(flightA, "JUMP_5");
        jMap6 = loadMap(flightA, "JUMP_6");
        sMap0 = loadMap(flightA, "SLOW_FALLING_JUMP_0");
        sMap1 = loadMap(flightA, "SLOW_FALLING_JUMP_1");
        sMap2 = loadMap(flightA, "SLOW_FALLING_JUMP_2");
        sMap3 = loadMap(flightA, "SLOW_FALLING_JUMP_3");
        sMap4 = loadMap(flightA, "SLOW_FALLING_JUMP_4");
        sMap5 = loadMap(flightA, "SLOW_FALLING_JUMP_5");
        sMap6 = loadMap(flightA, "SLOW_FALLING_JUMP_6");

        String flightB = readResource("flight-b.json");
        heightLimits = loadMap(flightB, "HEIGHT_LIMITS");

        String boatA = readResource("boat-a.json");
        boatSpeeds = loadMap(boatA, "SPEEDS");
    }

    private static String readResource(
            final String fileName
    ) throws IOException {
        final String resourcePath =
                FIXTURE_ROOT + fileName;

        final InputStream resource =
                VanillaVerticalPhysicsTest.class
                        .getResourceAsStream(resourcePath);

        if (resource == null) {
            throw new IOException(
                    "Missing test fixture resource: "
                            + resourcePath
            );
        }

        try (
                InputStream input = resource;
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            final byte[] buffer = new byte[4096];
            int read;

            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            return new String(
                    output.toByteArray(),
                    StandardCharsets.UTF_8
            );
        }
    }

    /**
     * Extracts the {"k":N,"v":V} pairs of the named map's "entries" array using
     * regex (no external JSON dependency). Optional whitespace is tolerated.
     */
    private static Map<Integer, Double> loadMap(String json, String mapName) {
        Map<Integer, Double> result = new HashMap<Integer, Double>();
        Pattern mapPattern = Pattern.compile(
                "\"" + Pattern.quote(mapName) + "\"\\s*:\\s*\\{", Pattern.DOTALL);
        Matcher mapMatcher = mapPattern.matcher(json);
        assertTrue(mapMatcher.find(), "map not found: " + mapName);
        int start = mapMatcher.end();
        Pattern entriesPattern = Pattern.compile("\"entries\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher entriesMatcher = entriesPattern.matcher(json.substring(start));
        assertTrue(entriesMatcher.find(), "entries not found for: " + mapName);
        String array = entriesMatcher.group(1);
        Pattern pairPattern = Pattern.compile(
                "\\{\"k\"\\s*:\\s*(\\d+)\\s*,\\s*\"v\"\\s*:\\s*([\\d.Ee+-]+)\\}");
        Matcher pairMatcher = pairPattern.matcher(array);
        int count = 0;
        while (pairMatcher.find()) {
            int k = Integer.parseInt(pairMatcher.group(1));
            double v = Double.parseDouble(pairMatcher.group(2));
            result.put(k, v);
            count++;
        }
        assertTrue(count > 0, "no entries parsed for: " + mapName);
        return result;
    }

    private static void assertEachEntry(Map<Integer, Double> map, int amp, boolean slow) {
        for (Map.Entry<Integer, Double> e : map.entrySet()) {
            int k = e.getKey();
            double expected = e.getValue();
            double actual = VanillaVerticalPhysics.flightVerticalLimit(amp, k, slow);
            if (Double.isNaN(expected)) {
                continue;
            }
            assertTrue(Math.abs(actual - expected) <= EPS,
                    "mismatch amp=" + amp + " k=" + k + " expected=" + expected + " actual=" + actual);
        }
    }

    @Test
    public void flightNormalMatchesEveryExtractedEntry() {
        assertEachEntry(jMap0, 0, false);
        assertEachEntry(jMap1, 1, false);
        assertEachEntry(jMap2, 2, false);
        assertEachEntry(jMap3, 3, false);
        assertEachEntry(jMap4, 4, false);
        assertEachEntry(jMap5, 5, false);
        assertEachEntry(jMap6, 6, false);
    }

    @Test
    public void flightSlowMatchesEveryExtractedEntry() {
        assertEachEntry(sMap0, 0, true);
        assertEachEntry(sMap1, 1, true);
        assertEachEntry(sMap2, 2, true);
        assertEachEntry(sMap3, 3, true);
        assertEachEntry(sMap4, 4, true);
        assertEachEntry(sMap5, 5, true);
        assertEachEntry(sMap6, 6, true);
    }

    @Test
    public void flightNormalMissingKeysUseMinusPointFive() {
        int[] max = {25, 27, 29, 30, 32, 34, 35};
        for (int amp = 0; amp <= 6; amp++) {
            double beyond = VanillaVerticalPhysics.flightVerticalLimit(amp, max[amp] + 5, false);
            assertEquals(-0.5D, beyond, EPS, "amp=" + amp + " beyond-max should be -0.5");
            double nonPositive = VanillaVerticalPhysics.flightVerticalLimit(amp, 0, false);
            assertEquals(-0.5D, nonPositive, EPS, "amp=" + amp + " tick<=0 should be -0.5");
        }
    }

    @Test
    public void flightSlowMissingKeysUseMinusPointTwo() {
        int[] max = {73, 77, 81, 85, 87, 91, 95};
        for (int amp = 0; amp <= 6; amp++) {
            double beyond = VanillaVerticalPhysics.flightVerticalLimit(amp, max[amp] + 5, true);
            assertEquals(-0.2D, beyond, EPS, "amp=" + amp + " beyond-max slow should be -0.2");
            double nonPositive = VanillaVerticalPhysics.flightVerticalLimit(amp, 0, true);
            assertEquals(-0.2D, nonPositive, EPS, "amp=" + amp + " tick<=0 slow should be -0.2");
        }
    }

    @Test
    public void unknownAmplifierUsesProfileSix() {
        Integer aValidKey = jMap6.keySet().iterator().next();
        double expected = VanillaVerticalPhysics.flightVerticalLimit(6, aValidKey, false);
        double actual = VanillaVerticalPhysics.flightVerticalLimit(99, aValidKey, false);
        assertTrue(Math.abs(actual - expected) <= EPS,
                "amp=99 should map to profile 6 for valid key");
        assertEquals(-0.5D, VanillaVerticalPhysics.flightVerticalLimit(99, 100000, false), EPS,
                "amp=99 beyond-max should be -0.5");
    }

    @Test
    public void maxJumpHeightMatchesAllThirtyThreeEntries() {
        assertEquals(33, heightLimits.size(), "expected 33 height-limit entries");
        for (Map.Entry<Integer, Double> e : heightLimits.entrySet()) {
            int k = e.getKey();
            double expected = e.getValue();
            double actual = VanillaVerticalPhysics.maxJumpHeight(k);
            assertTrue(Math.abs(actual - expected) <= EPS,
                    "maxJumpHeight mismatch k=" + k + " expected=" + expected + " actual=" + actual);
        }
    }

    @Test
    public void maxJumpHeightOutOfRangeIsUnbounded() {
        assertEquals(Double.MAX_VALUE, VanillaVerticalPhysics.maxJumpHeight(-1), EPS);
        assertEquals(Double.MAX_VALUE, VanillaVerticalPhysics.maxJumpHeight(33), EPS);
    }

    @Test
    public void boatSpeedMatchesAllSeventeenEntries() {
        assertEquals(17, boatSpeeds.size(), "expected 17 boat-speed entries (k=4..20)");
        for (Map.Entry<Integer, Double> e : boatSpeeds.entrySet()) {
            int k = e.getKey();
            double expected = e.getValue();
            double actual = VanillaVerticalPhysics.boatVerticalSpeed(k);
            assertTrue(Math.abs(actual - expected) <= EPS,
                    "boatVerticalSpeed mismatch k=" + k + " expected=" + expected + " actual=" + actual);
        }
    }

    @Test
    public void boatSpeedAboveRangeUsesEventTwenty() {
        double at20 = VanillaVerticalPhysics.boatVerticalSpeed(20);
        assertEquals(at20, VanillaVerticalPhysics.boatVerticalSpeed(50), EPS);
        assertEquals(at20, VanillaVerticalPhysics.boatVerticalSpeed(20), EPS);
    }
}
