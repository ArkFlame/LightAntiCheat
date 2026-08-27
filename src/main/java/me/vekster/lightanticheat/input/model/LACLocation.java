package me.vekster.lightanticheat.input.model;

import java.util.Objects;
import java.util.UUID;

public final class LACLocation {

    private final UUID worldId;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public LACLocation(UUID worldId, double x, double y, double z, float yaw, float pitch) {
        if (worldId == null) {
            throw new IllegalArgumentException("worldId must not be null");
        }
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public LACLocation withPosition(double x, double y, double z) {
        return new LACLocation(worldId, x, y, z, yaw, pitch);
    }

    public LACLocation withRotation(float yaw, float pitch) {
        return new LACLocation(worldId, x, y, z, yaw, pitch);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LACLocation)) return false;
        LACLocation other = (LACLocation) object;
        return Double.compare(other.x, x) == 0
                && Double.compare(other.y, y) == 0
                && Double.compare(other.z, z) == 0
                && Float.compare(other.yaw, yaw) == 0
                && Float.compare(other.pitch, pitch) == 0
                && Objects.equals(worldId, other.worldId);
    }

    @Override
    public int hashCode() {
        int result = worldId != null ? worldId.hashCode() : 0;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (yaw != +0.0f ? Float.floatToIntBits(yaw) : 0);
        result = 31 * result + (pitch != +0.0f ? Float.floatToIntBits(pitch) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LACLocation{worldId=" + worldId + ", x=" + x + ", y=" + y + ", z=" + z + ", yaw=" + yaw + ", pitch=" + pitch + '}';
    }
}
