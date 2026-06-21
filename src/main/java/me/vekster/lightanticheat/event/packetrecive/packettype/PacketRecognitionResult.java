package me.vekster.lightanticheat.event.packetrecive.packettype;

public class PacketRecognitionResult {

    private final PacketType packetType;
    private final int entityId;

    public PacketRecognitionResult(PacketType packetType, int entityId) {
        this.packetType = packetType != null ? packetType : PacketType.OTHER;
        this.entityId = entityId;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public int getEntityId() {
        return entityId;
    }

}
