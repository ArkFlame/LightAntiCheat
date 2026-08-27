package me.vekster.lightanticheat.input.model;

import java.util.Objects;

public final class LACMovementFrame {

    private final LACPlayerSession session;
    private final long inputEpoch;
    private final long sequence;
    private final LACLocation from;
    private final LACLocation to;
    private final boolean positionChanged;
    private final boolean rotationChanged;
    private final boolean claimedOnGround;
    private final long receivedAtMillis;

    public LACMovementFrame(LACPlayerSession session,
                            long inputEpoch,
                            long sequence,
                            LACLocation from,
                            LACLocation to,
                            boolean positionChanged,
                            boolean rotationChanged,
                            boolean claimedOnGround,
                            long receivedAtMillis) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        if (from == null) {
            throw new IllegalArgumentException("from must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("to must not be null");
        }
        if (inputEpoch < 1L) {
            throw new IllegalArgumentException("inputEpoch must be >= 1");
        }
        if (sequence < 0L) {
            throw new IllegalArgumentException("sequence must be >= 0");
        }
        this.session = session;
        this.inputEpoch = inputEpoch;
        this.sequence = sequence;
        this.from = from;
        this.to = to;
        this.positionChanged = positionChanged;
        this.rotationChanged = rotationChanged;
        this.claimedOnGround = claimedOnGround;
        this.receivedAtMillis = receivedAtMillis;
    }

    public LACPlayerSession getSession() {
        return session;
    }

    public long getInputEpoch() {
        return inputEpoch;
    }

    public long getSequence() {
        return sequence;
    }

    public LACLocation getFrom() {
        return from;
    }

    public LACLocation getTo() {
        return to;
    }

    public boolean isPositionChanged() {
        return positionChanged;
    }

    public boolean isRotationChanged() {
        return rotationChanged;
    }

    public boolean isClaimedOnGround() {
        return claimedOnGround;
    }

    public long getReceivedAtMillis() {
        return receivedAtMillis;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LACMovementFrame)) return false;
        LACMovementFrame other = (LACMovementFrame) object;
        return inputEpoch == other.inputEpoch
                && sequence == other.sequence
                && positionChanged == other.positionChanged
                && rotationChanged == other.rotationChanged
                && claimedOnGround == other.claimedOnGround
                && receivedAtMillis == other.receivedAtMillis
                && Objects.equals(session, other.session)
                && Objects.equals(from, other.from)
                && Objects.equals(to, other.to);
    }

    @Override
    public int hashCode() {
        int result = session != null ? session.hashCode() : 0;
        result = 31 * result + (int) (inputEpoch ^ (inputEpoch >>> 32));
        result = 31 * result + (int) (sequence ^ (sequence >>> 32));
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (positionChanged ? 1 : 0);
        result = 31 * result + (rotationChanged ? 1 : 0);
        result = 31 * result + (claimedOnGround ? 1 : 0);
        result = 31 * result + (int) (receivedAtMillis ^ (receivedAtMillis >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "LACMovementFrame{session=" + session + ", inputEpoch=" + inputEpoch + ", sequence=" + sequence
                + ", from=" + from + ", to=" + to
                + ", positionChanged=" + positionChanged + ", rotationChanged=" + rotationChanged
                + ", claimedOnGround=" + claimedOnGround + ", receivedAtMillis=" + receivedAtMillis + '}';
    }
}
