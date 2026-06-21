package me.vekster.lightanticheat.event.packetrecive.packettype;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PacketTypeRecognizer {

    private static final ConcurrentMap<Class<?>, PacketMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    private static class PacketMetadata {
        final PacketType packetType;
        final Field entityIdField;

        PacketMetadata(PacketType packetType, Field entityIdField) {
            this.packetType = packetType;
            this.entityIdField = entityIdField;
        }
    }

    public static PacketRecognitionResult recognize(Object nmsPacket) {
        Class<?> clazz = nmsPacket.getClass();
        PacketMetadata meta = METADATA_CACHE.computeIfAbsent(clazz, PacketTypeRecognizer::resolveMetadata);
        if (meta.entityIdField == null) {
            return new PacketRecognitionResult(meta.packetType, 0);
        }
        try {
            int entityId = meta.entityIdField.getInt(nmsPacket);
            return new PacketRecognitionResult(meta.packetType, entityId);
        } catch (IllegalAccessException e) {
            return new PacketRecognitionResult(meta.packetType, 0);
        }
    }

    public static PacketType getPacketType(Object nmsPacket) {
        return recognize(nmsPacket).getPacketType();
    }

    public static int getEntityId(Object nmsPacket) {
        return recognize(nmsPacket).getEntityId();
    }

    private static String getSimpleClassName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        int dollarIndex = name.indexOf('$');
        if (dollarIndex != -1) {
            name = name.substring(0, dollarIndex);
        }
        return name;
    }

    private static PacketMetadata resolveMetadata(Class<?> clazz) {
        String simpleName = getSimpleClassName(clazz);
        PacketType packetType;
        if ("PacketPlayInFlying".equals(simpleName)) {
            packetType = PacketType.FLYING;
        } else if ("PacketPlayInArmAnimation".equals(simpleName)) {
            packetType = PacketType.ARM_ANIMATION;
        } else if ("PacketPlayInBlockDig".equals(simpleName)) {
            packetType = PacketType.BLOCK_DIG;
        } else if ("PacketPlayInSteerVehicle".equals(simpleName)) {
            packetType = PacketType.STEER_VEHICLE;
        } else if ("PacketPlayInSetCreativeSlot".equals(simpleName)) {
            packetType = PacketType.SET_CREATIVE_SLOT;
        } else if ("ServerboundClientInformationPacket".equals(simpleName)) {
            packetType = PacketType.CLIENT_INFORMATION;
        } else if ("ServerboundKeepAlivePacket".equals(simpleName)) {
            packetType = PacketType.ALIVE;
        } else if ("PacketPlayInUseEntity".equals(simpleName)) {
            packetType = PacketType.USE_ENTITY;
        } else {
            packetType = PacketType.OTHER;
        }

        Field entityIdField = null;
        if (packetType == PacketType.USE_ENTITY) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == int.class) {
                    entityIdField = field;
                    entityIdField.setAccessible(true);
                    break;
                }
            }
        }

        return new PacketMetadata(packetType, entityIdField);
    }

}
