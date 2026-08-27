package me.vekster.lightanticheat.input.provider.nms;

import me.vekster.lightanticheat.input.model.LACPacketType;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NmsPacketRecognizer {

    private static final ConcurrentMap<Class<?>, PacketMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    private static final class PacketMetadata {
        final LACPacketType packetType;
        final Field entityIdField;

        PacketMetadata(LACPacketType packetType, Field entityIdField) {
            this.packetType = packetType;
            this.entityIdField = entityIdField;
        }
    }

    public static final class Result {
        private final LACPacketType packetType;
        private final int entityId;

        public Result(LACPacketType packetType, int entityId) {
            this.packetType = packetType != null ? packetType : LACPacketType.OTHER;
            this.entityId = entityId;
        }

        public LACPacketType getPacketType() {
            return packetType;
        }

        public int getEntityId() {
            return entityId;
        }
    }

    private NmsPacketRecognizer() {
    }

    public static Result recognize(Object nmsPacket) {
        if (nmsPacket == null) {
            return new Result(LACPacketType.OTHER, 0);
        }
        Class<?> clazz = nmsPacket.getClass();
        PacketMetadata meta = METADATA_CACHE.computeIfAbsent(clazz, NmsPacketRecognizer::resolveMetadata);
        if (meta.entityIdField == null) {
            return new Result(meta.packetType, 0);
        }
        try {
            int entityId = meta.entityIdField.getInt(nmsPacket);
            return new Result(meta.packetType, entityId);
        } catch (IllegalAccessException e) {
            return new Result(meta.packetType, 0);
        }
    }

    public static LACPacketType getPacketType(Object nmsPacket) {
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
        LACPacketType packetType;
        if ("PacketPlayInFlying".equals(simpleName)) {
            packetType = LACPacketType.FLYING;
        } else if ("PacketPlayInArmAnimation".equals(simpleName)) {
            packetType = LACPacketType.ARM_ANIMATION;
        } else if ("PacketPlayInBlockDig".equals(simpleName)) {
            packetType = LACPacketType.BLOCK_DIG;
        } else if ("PacketPlayInSteerVehicle".equals(simpleName)) {
            packetType = LACPacketType.STEER_VEHICLE;
        } else if ("PacketPlayInSetCreativeSlot".equals(simpleName)) {
            packetType = LACPacketType.SET_CREATIVE_SLOT;
        } else if ("ServerboundClientInformationPacket".equals(simpleName)) {
            packetType = LACPacketType.CLIENT_INFORMATION;
        } else if ("ServerboundKeepAlivePacket".equals(simpleName)) {
            packetType = LACPacketType.ALIVE;
        } else if ("PacketPlayInUseEntity".equals(simpleName)) {
            packetType = LACPacketType.USE_ENTITY;
        } else {
            packetType = LACPacketType.OTHER;
        }

        Field entityIdField = null;
        if (packetType == LACPacketType.USE_ENTITY) {
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
