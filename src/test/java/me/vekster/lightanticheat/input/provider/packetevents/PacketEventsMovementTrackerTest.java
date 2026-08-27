package me.vekster.lightanticheat.input.provider.packetevents;

import com.github.retrooper.packetevents.protocol.world.Location;
import me.vekster.lightanticheat.input.model.LACLocation;
import me.vekster.lightanticheat.input.model.LACMovementFrame;
import me.vekster.lightanticheat.input.model.LACPlayerSession;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PacketEventsMovementTrackerTest {

    private static LACLocation loc(UUID worldId, double x, double y, double z, float yaw, float pitch) {
        return new LACLocation(worldId, x, y, z, yaw, pitch);
    }

    private static Location raw(double x, double y, double z, float yaw, float pitch) {
        return new Location(x, y, z, yaw, pitch);
    }

    @Test
    void positionAndRotationFrameSeedsAllComponents() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 0f, 0f);
        Location incoming = raw(10, 65, 20, 90f, 45f);

        Optional<LACMovementFrame> opt = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, incoming, seed);

        assertTrue(opt.isPresent());
        LACMovementFrame f = opt.get();
        assertEquals(loc(wid, 10, 65, 20, 90f, 45f), f.getTo());
    }

    @Test
    void rotationOnlyPacketPreservesXyz() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 10f, 5f);

        // seed -> first frame establishes baseline with position+rotation
        Optional<LACMovementFrame> first = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        assertTrue(first.isPresent());

        // rotation-only: x/y/z must stay as previous to
        Optional<LACMovementFrame> second = tracker.trackOrUpdate(session, 1L, 2L, false, true, true, raw(999, 999, 999, 180f, 30f), null);

        assertTrue(second.isPresent());
        assertEquals(10.0, second.get().getTo().getX());
    }

    @Test
    void positionOnlyPacketPreservesYawPitch() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 10f, 5f);

        Optional<LACMovementFrame> first = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        assertTrue(first.isPresent());

        Optional<LACMovementFrame> second = tracker.trackOrUpdate(session, 1L, 2L, true, false, true, raw(11, 66, 21, 999f, 999f), null);

        assertTrue(second.isPresent());
        assertEquals(90f, second.get().getTo().getYaw());
    }

    @Test
    void flyingFlagsOnlyPacketPreservesBoth() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> first = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        assertTrue(first.isPresent());
        LACLocation baseline = first.get().getTo();

        Optional<LACMovementFrame> second = tracker.trackOrUpdate(session, 1L, 2L, false, false, true, raw(999, 999, 999, 999f, 999f), null);

        assertTrue(second.isPresent());
        assertEquals(baseline, second.get().getTo());
    }

    @Test
    void epochChangeResetsOldTimeline() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session1 = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed1 = loc(wid, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> first = tracker.trackOrUpdate(session1, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed1);
        assertTrue(first.isPresent());

        // epoch 2 should reset: from must be new seed, not previous to
        LACPlayerSession session2 = new LACPlayerSession(pid, wid, 2L);
        LACLocation seed2 = loc(wid, 100, 70, 100, 0f, 0f);
        Optional<LACMovementFrame> second = tracker.trackOrUpdate(session2, 2L, 1L, true, true, true, raw(101, 71, 101, 10f, 10f), seed2);

        assertTrue(second.isPresent());
        assertEquals(seed2, second.get().getFrom());
    }

    @Test
    void worldChangeResetsOldTimeline() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid1 = UUID.randomUUID();
        UUID wid2 = UUID.randomUUID();
        LACPlayerSession session1 = new LACPlayerSession(pid, wid1, 1L);
        LACLocation seed1 = loc(wid1, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> first = tracker.trackOrUpdate(session1, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed1);
        assertTrue(first.isPresent());

        LACPlayerSession session2 = new LACPlayerSession(pid, wid2, 1L);
        LACLocation seed2 = loc(wid2, 200, 80, 200, 0f, 0f);
        Optional<LACMovementFrame> second = tracker.trackOrUpdate(session2, 1L, 1L, true, true, true, raw(201, 81, 201, 20f, 20f), seed2);

        assertTrue(second.isPresent());
        assertEquals(seed2, second.get().getFrom());
    }

    @Test
    void sequenceIncreasesMonotonically() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> f1 = tracker.trackOrUpdate(session, 1L, -1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        Optional<LACMovementFrame> f2 = tracker.trackOrUpdate(session, 1L, -1L, true, true, true, raw(11, 65, 20, 90f, 45f), null);
        Optional<LACMovementFrame> f3 = tracker.trackOrUpdate(session, 1L, -1L, true, true, true, raw(12, 65, 20, 90f, 45f), null);

        assertTrue(f1.isPresent() && f2.isPresent() && f3.isPresent());
        assertTrue(f2.get().getSequence() > f1.get().getSequence() && f3.get().getSequence() > f2.get().getSequence());
    }

    @Test
    void nanPositionRejectsWithoutCorruptingPriorState() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> valid = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        assertTrue(valid.isPresent());

        Optional<LACMovementFrame> bad = tracker.trackOrUpdate(session, 1L, 2L, true, true, true, raw(Double.NaN, 65, 20, 90f, 45f), null);

        assertTrue(!bad.isPresent());
    }

    @Test
    void infinitePositionRejects() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> valid = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        assertTrue(valid.isPresent());

        Optional<LACMovementFrame> bad = tracker.trackOrUpdate(session, 1L, 2L, true, true, true, raw(Double.POSITIVE_INFINITY, 65, 20, 90f, 45f), null);

        assertTrue(!bad.isPresent());
    }

    @Test
    void finiteNextPacketAfterMalformedInputContinuesFromLastValidState() {
        PacketEventsMovementTracker tracker = new PacketEventsMovementTracker();
        UUID pid = UUID.randomUUID();
        UUID wid = UUID.randomUUID();
        LACPlayerSession session = new LACPlayerSession(pid, wid, 1L);
        LACLocation seed = loc(wid, 0, 64, 0, 0f, 0f);

        Optional<LACMovementFrame> valid = tracker.trackOrUpdate(session, 1L, 1L, true, true, true, raw(10, 65, 20, 90f, 45f), seed);
        assertTrue(valid.isPresent());
        LACLocation lastValidTo = valid.get().getTo();

        // malformed rejected
        Optional<LACMovementFrame> bad = tracker.trackOrUpdate(session, 1L, 2L, true, true, true, raw(Double.NaN, 65, 20, 90f, 45f), null);
        assertTrue(!bad.isPresent());

        // next finite must continue from last valid
        Optional<LACMovementFrame> next = tracker.trackOrUpdate(session, 1L, 3L, true, true, true, raw(11, 65, 20, 90f, 45f), null);

        assertTrue(next.isPresent());
        assertEquals(lastValidTo, next.get().getFrom());
    }
}
