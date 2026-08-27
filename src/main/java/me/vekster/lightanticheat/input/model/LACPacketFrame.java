package me.vekster.lightanticheat.input.model;

import java.util.Objects;
import java.util.Optional;

public final class LACPacketFrame {

    private final LACPlayerSession session;
    private final long inputEpoch;
    private final long sequence;
    private final LACPacketType packetType;
    private final int entityId;
    private final Optional<LACLocation> movementLocation;
    private final long receivedAtMillis;

    public LACPacketFrame(LACPlayerSession session,
                          long inputEpoch,
                          long sequence,
                          LACPacketType packetType,
                          int entityId,
                          Optional<LACLocation> movementLocation,
                          long receivedAtMillis) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        if (packetType == null) {
            throw new IllegalArgumentException("packetType must not be null");
        }
        if (movementLocation == null) {
            throw new IllegalArgumentException("movementLocation must not be null");
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
        this.packetType = packetType;
        this.entityId = entityId;
        this.movementLocation = movementLocation;
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

    public LACPacketType getPacketType() {
        return packetType;
    }

    public int getEntityId() {
        return entityId;
    }

    public Optional<LACLocation> getMovementLocation() {
        return movementLocation;
    }

    public long getReceivedAtMillis() {
        return receivedAtMillis;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LACPacketFrame)) return false;
        LACPacketFrame other = (LACPacketFrame) object;
        return inputEpoch == other.inputEpoch
                && sequence == other.sequence
                && entityId == other.entityId
                && receivedAtMillis == other.receivedAtMillis
                && Objects.equals(session, other.session)
                && packetType == other.packetType
                && Objects.equals(movementLocation, other.movementLocation);
    }

    @Override
    public int hashCode() {
        int result = session != null ? session.hashCode() : 0;
        result = 31 * result + (int) (inputEpoch ^ (inputEpoch >>> 32));
        result = 31 * result + (int) (sequence ^ (sequence >>> 32));
        result = 31 * result + (packetType != null ? packetType.hashCode() : 0);
        result = 31 * result + entityId;
        result = 31 * result + (movementLocation != null ? movementLocation.hashCode() : 0);
        result = 31 * result + (int) (receivedAtMillis ^ (receivedAtMillis >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "LACPacketFrame{session=" + session + ", inputEpoch=" + inputEpoch + ", sequence=" + sequence
                + ", packetType=" + packetType + ", entityId=" + entityId
                + ", movementLocation=" + movementLocation + ", receivedAtMillis=" + receivedAtMillis + '}';
    }
}
