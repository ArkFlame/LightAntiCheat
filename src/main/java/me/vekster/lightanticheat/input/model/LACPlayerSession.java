package me.vekster.lightanticheat.input.model;

import java.util.Objects;
import java.util.UUID;

public final class LACPlayerSession {

    private final UUID playerId;
    private final UUID worldId;
    private final long playerEpoch;

    public LACPlayerSession(UUID playerId, UUID worldId, long playerEpoch) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId must not be null");
        }
        if (worldId == null) {
            throw new IllegalArgumentException("worldId must not be null");
        }
        if (playerEpoch < 1L) {
            throw new IllegalArgumentException("playerEpoch must be >= 1");
        }
        this.playerId = playerId;
        this.worldId = worldId;
        this.playerEpoch = playerEpoch;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public long getPlayerEpoch() {
        return playerEpoch;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LACPlayerSession)) return false;
        LACPlayerSession other = (LACPlayerSession) object;
        return playerEpoch == other.playerEpoch
                && Objects.equals(playerId, other.playerId)
                && Objects.equals(worldId, other.worldId);
    }

    @Override
    public int hashCode() {
        int result = playerId != null ? playerId.hashCode() : 0;
        result = 31 * result + (worldId != null ? worldId.hashCode() : 0);
        result = 31 * result + (int) (playerEpoch ^ (playerEpoch >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "LACPlayerSession{playerId=" + playerId + ", worldId=" + worldId + ", playerEpoch=" + playerEpoch + '}';
    }
}
