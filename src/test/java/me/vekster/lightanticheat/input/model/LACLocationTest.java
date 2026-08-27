package me.vekster.lightanticheat.input.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class LACLocationTest {

    private static LACLocation loc(UUID worldId, double x, double y, double z, float yaw, float pitch) {
        return new LACLocation(worldId, x, y, z, yaw, pitch);
    }

    @Test
    void equalitySameValuesReturnsTrue() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation a = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertTrue(result);
    }

    @Test
    void inequalityByWorldReturnsFalse() {
        // Arrange
        UUID worldA = UUID.randomUUID();
        UUID worldB = UUID.randomUUID();
        LACLocation a = loc(worldA, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldB, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void inequalityByXReturnsFalse() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation a = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldId, 9.0, 2.0, 3.0, 10.0f, 20.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void inequalityByYReturnsFalse() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation a = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldId, 1.0, 9.0, 3.0, 10.0f, 20.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void inequalityByZReturnsFalse() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation a = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldId, 1.0, 2.0, 9.0, 10.0f, 20.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void inequalityByYawReturnsFalse() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation a = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldId, 1.0, 2.0, 3.0, 90.0f, 20.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void inequalityByPitchReturnsFalse() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation a = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 20.0f);
        LACLocation b = loc(worldId, 1.0, 2.0, 3.0, 10.0f, 90.0f);
        // Act
        boolean result = a.equals(b);
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void withPositionPreservesRotationAndWorldAndOriginalUnchanged() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation original = loc(worldId, 1.0, 2.0, 3.0, 30.0f, 40.0f);
        // Act
        LACLocation moved = original.withPosition(10.0, 20.0, 30.0);
        // Assert
        Assertions.assertTrue(
                moved.getX() == 10.0
                        && moved.getY() == 20.0
                        && moved.getZ() == 30.0
                        && moved.getYaw() == 30.0f
                        && moved.getPitch() == 40.0f
                        && moved.getWorldId().equals(worldId)
                        && original.getX() == 1.0
                        && original.getY() == 2.0
                        && original.getZ() == 3.0
                        && original.getYaw() == 30.0f
                        && original.getPitch() == 40.0f
                        && original.getWorldId().equals(worldId)
        );
    }

    @Test
    void withRotationPreservesPositionAndWorldAndOriginalUnchanged() {
        // Arrange
        UUID worldId = UUID.randomUUID();
        LACLocation original = loc(worldId, 1.0, 2.0, 3.0, 30.0f, 40.0f);
        // Act
        LACLocation rotated = original.withRotation(90.0f, 60.0f);
        // Assert
        Assertions.assertTrue(
                rotated.getYaw() == 90.0f
                        && rotated.getPitch() == 60.0f
                        && rotated.getX() == 1.0
                        && rotated.getY() == 2.0
                        && rotated.getZ() == 3.0
                        && rotated.getWorldId().equals(worldId)
                        && original.getYaw() == 30.0f
                        && original.getPitch() == 40.0f
                        && original.getX() == 1.0
                        && original.getY() == 2.0
                        && original.getZ() == 3.0
                        && original.getWorldId().equals(worldId)
        );
    }
}
