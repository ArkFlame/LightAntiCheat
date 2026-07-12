package me.vekster.lightanticheat.check.checks.movement.nofall;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JSON golden fixtures are stored under
 * src/test/resources/lac-quality-extract and loaded through the
 * test classpath. The ordered distance entries remain frozen in
 * their original order.
 */
public class NoFallPredictionProfileTest {

    private static final String FIXTURE_ROOT = "/lac-quality-extract/";

    private static final int[][] EVENTS_KEYS = new int[6][];
    private static final float[][] EVENTS_VALUES = new float[6][];
    private static final double[][] DISTANCE_THRESHOLDS = new double[6][];
    private static final float[][] DISTANCE_LIMITS = new float[6][];

    @BeforeAll
    public static void loadFixtures() throws IOException {
        String eventsJson = readResource("nofall-events.json");
        for (int i = 0; i < 6; i++) {
            List<EntryIntFloat> entries = loadEntriesIntFloat(eventsJson, "EVENTS_JUMP_" + i);
            EVENTS_KEYS[i] = new int[entries.size()];
            EVENTS_VALUES[i] = new float[entries.size()];
            for (int j = 0; j < entries.size(); j++) {
                EVENTS_KEYS[i][j] = entries.get(j).key;
                EVENTS_VALUES[i][j] = entries.get(j).value;
            }
        }

        String distanceJson = readResource("nofall-distance.json");
        for (int i = 0; i < 6; i++) {
            List<EntryDoubleFloat> entries = loadEntriesDoubleFloat(distanceJson, "DISTANCE_JUMP_" + i);
            DISTANCE_THRESHOLDS[i] = new double[entries.size()];
            DISTANCE_LIMITS[i] = new float[entries.size()];
            for (int j = 0; j < entries.size(); j++) {
                DISTANCE_THRESHOLDS[i][j] = entries.get(j).key;
                DISTANCE_LIMITS[i][j] = entries.get(j).value;
            }
        }
    }

    private static String readResource(
            final String fileName
    ) throws IOException {
        final String resourcePath =
                FIXTURE_ROOT + fileName;

        final InputStream resource =
                NoFallPredictionProfileTest.class
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

    private static final class EntryIntFloat {
        final int key;
        final float value;

        EntryIntFloat(int k, float v) {
            this.key = k;
            this.value = v;
        }
    }

    private static final class EntryDoubleFloat {
        final double key;
        final float value;

        EntryDoubleFloat(double k, float v) {
            this.key = k;
            this.value = v;
        }
    }

    private static List<EntryIntFloat> loadEntriesIntFloat(String json, String mapName) {
        String array = extractEntriesArray(json, mapName);
        Pattern pairPattern = Pattern.compile(
                "\\{\"k\"\\s*:\\s*(\\d+)\\s*,\\s*\"v\"\\s*:\\s*([\\d.Ee+-]+)\\}");
        Matcher pairMatcher = pairPattern.matcher(array);
        List<EntryIntFloat> result = new ArrayList<EntryIntFloat>();
        while (pairMatcher.find()) {
            int k = Integer.parseInt(pairMatcher.group(1));
            float v = (float) Double.parseDouble(pairMatcher.group(2));
            result.add(new EntryIntFloat(k, v));
        }
        assertTrue(result.size() > 0, "no entries parsed for: " + mapName);
        return result;
    }

    private static List<EntryDoubleFloat> loadEntriesDoubleFloat(String json, String mapName) {
        String array = extractEntriesArray(json, mapName);
        Pattern pairPattern = Pattern.compile(
                "\\{\"k\"\\s*:\\s*(-?[\\d.Ee+-]+)\\s*,\\s*\"v\"\\s*:\\s*([\\d.Ee+-]+)\\}");
        Matcher pairMatcher = pairPattern.matcher(array);
        List<EntryDoubleFloat> result = new ArrayList<EntryDoubleFloat>();
        while (pairMatcher.find()) {
            double k = Double.parseDouble(pairMatcher.group(1));
            float v = (float) Double.parseDouble(pairMatcher.group(2));
            result.add(new EntryDoubleFloat(k, v));
        }
        assertTrue(result.size() > 0, "no entries parsed for: " + mapName);
        return result;
    }

    private static String extractEntriesArray(String json, String mapName) {
        Pattern mapPattern = Pattern.compile(
                "\"" + Pattern.quote(mapName) + "\"\\s*:\\s*\\{", Pattern.DOTALL);
        Matcher mapMatcher = mapPattern.matcher(json);
        assertTrue(mapMatcher.find(), "map not found: " + mapName);
        int start = mapMatcher.end();
        Pattern entriesPattern = Pattern.compile("\"entries\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher entriesMatcher = entriesPattern.matcher(json.substring(start));
        assertTrue(entriesMatcher.find(), "entries not found for: " + mapName);
        return entriesMatcher.group(1);
    }

    /**
     * Legacy ordered-lookup algorithm: iterate thresholds in insertion order;
     * for each i with abs(threshold_i) >= abs(fallDistance), track the
     * minimum limit_i; return -1F if no candidate. Implemented locally
     * against the fixture primitive arrays, NOT against production code.
     */
    private static float legacyByDistance(double[] thresholds, float[] limits, double query) {
        float best = -1F;
        for (int i = 0; i < thresholds.length; i++) {
            if (Math.abs(thresholds[i]) >= Math.abs(query)) {
                float v = limits[i];
                best = (best == -1F) ? v : Math.min(best, v);
            }
        }
        return best;
    }

    private static double absMin(double[] a) {
        double m = Math.abs(a[0]);
        for (int i = 1; i < a.length; i++) {
            double v = Math.abs(a[i]);
            if (v < m) m = v;
        }
        return m;
    }

    private static double absMax(double[] a) {
        double m = Math.abs(a[0]);
        for (int i = 1; i < a.length; i++) {
            double v = Math.abs(a[i]);
            if (v > m) m = v;
        }
        return m;
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    public void everyEventEntryMatchesExtractedFixtureBitExactly() {
        for (int amp = 0; amp < 6; amp++) {
            int[] keys = EVENTS_KEYS[amp];
            float[] values = EVENTS_VALUES[amp];
            for (int j = 0; j < keys.length; j++) {
                int k = keys[j];
                float expected = values[j];
                float actual = NoFallPredictionProfile.byEvents(amp, k);
                assertEquals(
                        Float.floatToIntBits(expected),
                        Float.floatToIntBits(actual),
                        "mismatch amp=" + amp + " k=" + k
                                + " expected=" + expected + " actual=" + actual);
            }
        }
    }

    @Test
    public void missingEventReturnsMinusOne() {
        // events >= EVENT_TABLE_SIZE (45) is always out of range.
        // events < 2 lands on the NaN-filled leading slots of every profile table.
        // events < 0 is short-circuited before the table lookup.
        int[] outOfRange = {-1, 0, 1, 45, 46, 99999};
        for (int events : outOfRange) {
            for (int amp = 0; amp < 6; amp++) {
                float v = NoFallPredictionProfile.byEvents(amp, events);
                assertEquals(
                        Float.floatToIntBits(-1F),
                        Float.floatToIntBits(v),
                        "amp=" + amp + " events=" + events + " should be -1F");
            }
        }
    }

    @Test
    public void amplifierAboveFiveUsesProfileFive() {
        int validK = 5;
        // amp=99 -> profile 5 (clamps above MAX_PROFILE_INDEX)
        assertEquals(
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(5, validK)),
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(99, validK)),
                "amp=99 should clamp to profile 5");
        // amp=6 -> profile 5 (just above the threshold)
        assertEquals(
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(5, validK)),
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(6, validK)),
                "amp=6 should clamp to profile 5");
        // amp=-3 -> profile 5 (negative clamps to MAX_PROFILE_INDEX per profileForAmplifier)
        assertEquals(
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(5, validK)),
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(-3, validK)),
                "amp=-3 should clamp to profile 5 (not profile 0)");
        // amp=-1 -> profile 5 (also clamps)
        assertEquals(
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(5, validK)),
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(-1, validK)),
                "amp=-1 should clamp to profile 5");
        // sanity: amp=0 uses profile 0 which differs from profile 5 at validK=5
        assertEquals(
                Float.floatToIntBits(2.0900621F),
                Float.floatToIntBits(NoFallPredictionProfile.byEvents(0, validK)),
                "amp=0 should use profile 0 (value 2.0900621F vs profile 5's 1.885763F)");
    }

    @Test
    public void everyDistanceLookupMatchesLegacyOrderedAlgorithm() {
        for (int amp = 0; amp < 6; amp++) {
            double[] thresholds = DISTANCE_THRESHOLDS[amp];
            float[] limits = DISTANCE_LIMITS[amp];
            List<Double> queries = new ArrayList<Double>();

            // Every exact threshold value (both positive and negative forms)
            for (int i = 0; i < thresholds.length; i++) {
                queries.add(thresholds[i]);
                queries.add(-thresholds[i]);
            }

            // Zero
            queries.add(0.0);

            // Midpoint between every adjacent absolute threshold
            for (int i = 0; i + 1 < thresholds.length; i++) {
                double mid = (Math.abs(thresholds[i]) + Math.abs(thresholds[i + 1])) / 2.0;
                queries.add(mid);
                queries.add(-mid);
            }

            // Smallest threshold minus epsilon (in absolute value)
            double smallestAbs = absMin(thresholds);
            queries.add(smallestAbs - 1e-9);
            queries.add(-(smallestAbs - 1e-9));

            // Largest threshold plus epsilon (in absolute value)
            double largestAbs = absMax(thresholds);
            queries.add(largestAbs + 1e-9);
            queries.add(-(largestAbs + 1e-9));

            for (int q = 0; q < queries.size(); q++) {
                double query = queries.get(q);
                float expected = legacyByDistance(thresholds, limits, query);
                float actual = NoFallPredictionProfile.byDistance(amp, query);
                assertEquals(
                        Float.floatToIntBits(expected),
                        Float.floatToIntBits(actual),
                        "mismatch amp=" + amp + " query=" + query
                                + " expected=" + expected + " actual=" + actual);
            }
        }
    }

    @Test
    public void distanceOutsideProfileReturnsMinusOne() {
        for (int amp = 0; amp < 6; amp++) {
            double[] thresholds = DISTANCE_THRESHOLDS[amp];
            double largestAbs = absMax(thresholds);
            double huge = largestAbs + 1000.0;
            assertEquals(
                    Float.floatToIntBits(-1F),
                    Float.floatToIntBits(NoFallPredictionProfile.byDistance(amp, huge)),
                    "amp=" + amp + " query=" + huge + " should return -1F");
            assertEquals(
                    Float.floatToIntBits(-1F),
                    Float.floatToIntBits(NoFallPredictionProfile.byDistance(amp, -huge)),
                    "amp=" + amp + " query=" + (-huge) + " should return -1F");
        }
    }

    @Test
    public void horizontalExemptionUsesExactBits() {
        assertTrue(NoFallPredictionProfile.isHorizontalFallDistanceExemption(
                NoFallPredictionProfile.HORIZONTAL_FALL_DISTANCE_EXEMPTION));
        assertTrue(NoFallPredictionProfile.isHorizontalFallDistanceExemption(0.5770024597644806D));
        // 0.0 must NOT match
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(0.0));
        // 1.0 must NOT match
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(1.0));
    }

    @Test
    public void nearbyDoubleDoesNotTriggerExemption() {
        // +1 ULP: 0.5770024597644807D
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(0.5770024597644807D));
        // Math.ulp-based neighbours of the constant
        double ulp = Math.ulp(NoFallPredictionProfile.HORIZONTAL_FALL_DISTANCE_EXEMPTION);
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(
                NoFallPredictionProfile.HORIZONTAL_FALL_DISTANCE_EXEMPTION + ulp));
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(
                NoFallPredictionProfile.HORIZONTAL_FALL_DISTANCE_EXEMPTION - ulp));
        // 1e-6 offset must NOT match
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(
                NoFallPredictionProfile.HORIZONTAL_FALL_DISTANCE_EXEMPTION + 1e-6));
        assertFalse(NoFallPredictionProfile.isHorizontalFallDistanceExemption(
                NoFallPredictionProfile.HORIZONTAL_FALL_DISTANCE_EXEMPTION - 1e-6));
    }
}