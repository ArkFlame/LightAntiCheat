package me.vekster.lightanticheat.input.provider.packetevents;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import me.vekster.lightanticheat.input.model.LACPacketType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PacketEventsPacketMapperTest {

    // --- flying variants via PacketEvents helper ---

    @Test
    void isFlyingReturnsTrueForPlayerPosition() {
        Assertions.assertTrue(WrapperPlayClientPlayerFlying.isFlying(PacketType.Play.Client.PLAYER_POSITION));
    }

    @Test
    void isFlyingReturnsTrueForPlayerPositionAndRotation() {
        Assertions.assertTrue(WrapperPlayClientPlayerFlying.isFlying(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION));
    }

    @Test
    void isFlyingReturnsTrueForPlayerRotation() {
        Assertions.assertTrue(WrapperPlayClientPlayerFlying.isFlying(PacketType.Play.Client.PLAYER_ROTATION));
    }

    @Test
    void isFlyingReturnsTrueForPlayerFlying() {
        Assertions.assertTrue(WrapperPlayClientPlayerFlying.isFlying(PacketType.Play.Client.PLAYER_FLYING));
    }

    @Test
    void playerPositionMapsToFlying() {
        Assertions.assertEquals(LACPacketType.FLYING, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLAYER_POSITION));
    }

    @Test
    void playerPositionAndRotationMapsToFlying() {
        Assertions.assertEquals(LACPacketType.FLYING, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION));
    }

    @Test
    void playerRotationMapsToFlying() {
        Assertions.assertEquals(LACPacketType.FLYING, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLAYER_ROTATION));
    }

    @Test
    void playerFlyingMapsToFlying() {
        Assertions.assertEquals(LACPacketType.FLYING, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLAYER_FLYING));
    }

    // --- direct mappings ---

    @Test
    void animationMapsToArmAnimation() {
        Assertions.assertEquals(LACPacketType.ARM_ANIMATION, PacketEventsPacketMapper.mapType(PacketType.Play.Client.ANIMATION));
    }

    @Test
    void playerDiggingMapsToBlockDig() {
        Assertions.assertEquals(LACPacketType.BLOCK_DIG, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLAYER_DIGGING));
    }

    @Test
    void steerVehicleMapsToSteerVehicle() {
        Assertions.assertEquals(LACPacketType.STEER_VEHICLE, PacketEventsPacketMapper.mapType(PacketType.Play.Client.STEER_VEHICLE));
    }

    @Test
    void playerInputMapsToSteerVehicle() {
        Assertions.assertEquals(LACPacketType.STEER_VEHICLE, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLAYER_INPUT));
    }

    @Test
    void creativeInventoryActionMapsToSetCreativeSlot() {
        Assertions.assertEquals(LACPacketType.SET_CREATIVE_SLOT, PacketEventsPacketMapper.mapType(PacketType.Play.Client.CREATIVE_INVENTORY_ACTION));
    }

    @Test
    void interactEntityMapsToUseEntity() {
        Assertions.assertEquals(LACPacketType.USE_ENTITY, PacketEventsPacketMapper.mapType(PacketType.Play.Client.INTERACT_ENTITY));
    }

    @Test
    void attackMapsToUseEntity() {
        Assertions.assertEquals(LACPacketType.USE_ENTITY, PacketEventsPacketMapper.mapType(PacketType.Play.Client.ATTACK));
    }

    @Test
    void clientSettingsMapsToClientInformation() {
        Assertions.assertEquals(LACPacketType.CLIENT_INFORMATION, PacketEventsPacketMapper.mapType(PacketType.Play.Client.CLIENT_SETTINGS));
    }

    @Test
    void keepAliveMapsToAlive() {
        Assertions.assertEquals(LACPacketType.ALIVE, PacketEventsPacketMapper.mapType(PacketType.Play.Client.KEEP_ALIVE));
    }

    // --- OTHER ---

    @Test
    void chatMessageMapsToOther() {
        Assertions.assertEquals(LACPacketType.OTHER, PacketEventsPacketMapper.mapType(PacketType.Play.Client.CHAT_MESSAGE));
    }

    @Test
    void pluginMessageMapsToOther() {
        Assertions.assertEquals(LACPacketType.OTHER, PacketEventsPacketMapper.mapType(PacketType.Play.Client.PLUGIN_MESSAGE));
    }

    @Test
    void unrelatedPacketIsNotFlying() {
        Assertions.assertFalse(WrapperPlayClientPlayerFlying.isFlying(PacketType.Play.Client.CHAT_MESSAGE));
    }

    // --- null handling ---

    @Test
    void nullPlayClientMapsToOther() {
        Assertions.assertEquals(LACPacketType.OTHER, PacketEventsPacketMapper.mapType((PacketType.Play.Client) null));
    }

    @Test
    void nullCommonMapsToOther() {
        Assertions.assertEquals(LACPacketType.OTHER, PacketEventsPacketMapper.mapType((PacketTypeCommon) null));
    }

    // --- PacketTypeCommon overload delegates correctly ---

    @Test
    void commonOverloadDelegatesFlying() {
        PacketTypeCommon common = PacketType.Play.Client.PLAYER_POSITION;
        Assertions.assertEquals(LACPacketType.FLYING, PacketEventsPacketMapper.mapType(common));
    }

    @Test
    void commonOverloadDelegatesAnimation() {
        PacketTypeCommon common = PacketType.Play.Client.ANIMATION;
        Assertions.assertEquals(LACPacketType.ARM_ANIMATION, PacketEventsPacketMapper.mapType(common));
    }

    @Test
    void commonOverloadDelegatesOther() {
        PacketTypeCommon common = PacketType.Play.Client.CHAT_MESSAGE;
        Assertions.assertEquals(LACPacketType.OTHER, PacketEventsPacketMapper.mapType(common));
    }
}
