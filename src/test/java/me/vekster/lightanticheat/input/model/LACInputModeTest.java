package me.vekster.lightanticheat.input.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class LACInputModeTest {

    @Test
    void parsePacketLowerCaseReturnsPacket() {
        // Arrange
        String raw = "packet";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.of(LACInputMode.PACKET), result);
    }

    @Test
    void parsePacketUpperCaseReturnsPacket() {
        // Arrange
        String raw = "PACKET";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.of(LACInputMode.PACKET), result);
    }

    @Test
    void parsePacketWithWhitespaceReturnsPacket() {
        // Arrange
        String raw = "  packet  ";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.of(LACInputMode.PACKET), result);
    }

    @Test
    void parseNmsLowerCaseReturnsNms() {
        // Arrange
        String raw = "nms";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.of(LACInputMode.NMS), result);
    }

    @Test
    void parseNmsUpperCaseReturnsNms() {
        // Arrange
        String raw = "NMS";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.of(LACInputMode.NMS), result);
    }

    @Test
    void parseNmsMixedCaseWithWhitespaceReturnsNms() {
        // Arrange
        String raw = "  nMs ";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.of(LACInputMode.NMS), result);
    }

    @Test
    void parseUnknownReturnsEmpty() {
        // Arrange
        String raw = "unknown";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void parseNullReturnsEmpty() {
        // Arrange
        String raw = null;
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void parseEmptyStringReturnsEmpty() {
        // Arrange
        String raw = "";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void parseBlankWhitespaceReturnsEmpty() {
        // Arrange
        String raw = "   ";
        // Act
        Optional<LACInputMode> result = LACInputMode.parse(raw);
        // Assert
        Assertions.assertEquals(Optional.empty(), result);
    }
}
