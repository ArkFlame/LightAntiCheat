package me.vekster.lightanticheat.input.provider.packetevents;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.vekster.lightanticheat.input.model.LACPacketType;

public final class PacketEventsPacketMapper {

    private PacketEventsPacketMapper() {
    }

    public static LACPacketType mapType(PacketType.Play.Client type) {
        if (type == null) {
            return LACPacketType.OTHER;
        }
        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            return LACPacketType.FLYING;
        }
        if (type == PacketType.Play.Client.ANIMATION) {
            return LACPacketType.ARM_ANIMATION;
        }
        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            return LACPacketType.BLOCK_DIG;
        }
        if (type == PacketType.Play.Client.STEER_VEHICLE || type == PacketType.Play.Client.PLAYER_INPUT) {
            return LACPacketType.STEER_VEHICLE;
        }
        if (type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            return LACPacketType.SET_CREATIVE_SLOT;
        }
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            return LACPacketType.USE_ENTITY;
        }
        if (type == PacketType.Play.Client.ATTACK) {
            return LACPacketType.USE_ENTITY;
        }
        if (type == PacketType.Play.Client.CLIENT_SETTINGS) {
            return LACPacketType.CLIENT_INFORMATION;
        }
        if (type == PacketType.Play.Client.KEEP_ALIVE) {
            return LACPacketType.ALIVE;
        }
        return LACPacketType.OTHER;
    }

    public static LACPacketType mapType(PacketTypeCommon type) {
        if (type instanceof PacketType.Play.Client) {
            return mapType((PacketType.Play.Client) type);
        }
        return LACPacketType.OTHER;
    }

    public static int extractEntityId(PacketReceiveEvent event, LACPacketType mapped) {
        if (event == null || mapped != LACPacketType.USE_ENTITY) {
            return 0;
        }
        PacketTypeCommon raw = event.getPacketType();
        if (raw == PacketType.Play.Client.INTERACT_ENTITY) {
            try {
                return new WrapperPlayClientInteractEntity(event).getEntityId();
            } catch (Exception ignored) {
                return 0;
            }
        }
        if (raw == PacketType.Play.Client.ATTACK) {
            try {
                return new WrapperPlayClientAttack(event).getEntityId();
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }
}
