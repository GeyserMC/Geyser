/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.network;

#include "io.netty.buffer.ByteBuf"
#include "org.cloudburstmc.protocol.bedrock.codec.BedrockCodec"
#include "org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper"
#include "org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer"
#include "org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobEquipmentSerializer_v291"
#include "org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MoveEntityAbsoluteSerializer_v291"
#include "org.cloudburstmc.protocol.bedrock.codec.v291.serializer.PlayerHotbarSerializer_v291"
#include "org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityLinkSerializer_v291"
#include "org.cloudburstmc.protocol.bedrock.codec.v390.serializer.PlayerSkinSerializer_v390"
#include "org.cloudburstmc.protocol.bedrock.codec.v419.serializer.MovePlayerSerializer_v419"
#include "org.cloudburstmc.protocol.bedrock.codec.v486.serializer.BossEventSerializer_v486"
#include "org.cloudburstmc.protocol.bedrock.codec.v557.serializer.SetEntityDataSerializer_v557"
#include "org.cloudburstmc.protocol.bedrock.codec.v662.serializer.SetEntityMotionSerializer_v662"
#include "org.cloudburstmc.protocol.bedrock.codec.v712.serializer.MobArmorEquipmentSerializer_v712"
#include "org.cloudburstmc.protocol.bedrock.codec.v748.serializer.InventoryContentSerializer_v748"
#include "org.cloudburstmc.protocol.bedrock.codec.v748.serializer.InventorySlotSerializer_v748"
#include "org.cloudburstmc.protocol.bedrock.codec.v776.serializer.BossEventSerializer_v776"
#include "org.cloudburstmc.protocol.bedrock.packet.AnvilDamagePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.BedrockPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.BossEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ClientCacheBlobStatusPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ClientCheatAbilityPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.CodeBuilderSourcePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.CraftingEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.CreatePhotoPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.DebugInfoPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.EditorNetworkPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityFallPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.GameTestRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LabTablePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MapCreateLockedCopyPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MapInfoRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MultiplayerSettingsPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PhotoInfoRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PhotoTransferPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerInputPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PurchaseReceiptPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.RefreshEntitlementsPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.RiderJumpPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.ScriptMessagePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SettingsCommandPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SimpleEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SubChunkRequestPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket"
#include "org.cloudburstmc.protocol.common.util.VarInts"


@SuppressWarnings("deprecation")
class CodecProcessor {
    

    @SuppressWarnings("rawtypes")
    private static final BedrockPacketSerializer ILLEGAL_SERIALIZER = new BedrockPacketSerializer<>() {
        override public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            throw new IllegalArgumentException("Server tried to send unused packet " + packet.getClass().getSimpleName() + "!");
        }

        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            throw new IllegalArgumentException("Client tried to send unused packet " + packet.getClass().getSimpleName() + "!");
        }
    };


    @SuppressWarnings("rawtypes")
    private static final BedrockPacketSerializer IGNORED_SERIALIZER = new BedrockPacketSerializer<>() {
        override public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
        }

        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
        }
    };

    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER_V748 = new InventoryContentSerializer_v748() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalArgumentException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    /*
     * Serializer that throws an exception when trying to deserialize InventorySlotPacket since server-auth inventory is used.
     */
    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER_V748 = new InventorySlotSerializer_v748() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalArgumentException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<MovePlayerPacket> MOVE_PLAYER_SERIALIZER = new MovePlayerSerializer_v419() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MovePlayerPacket packet) {
            throw new IllegalArgumentException("Client cannot send MovePlayerPacket in server-auth movement environment!");
        }
    };

    private static final BedrockPacketSerializer<MoveEntityAbsolutePacket> MOVE_ENTITY_SERIALIZER = new MoveEntityAbsoluteSerializer_v291() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MoveEntityAbsolutePacket packet) {
            throw new IllegalArgumentException("Client cannot send MoveEntityAbsolutePacket in server-auth movement environment!");
        }
    };


    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER_486 = new BossEventSerializer_v486() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER_776 = new BossEventSerializer_v776() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<MobArmorEquipmentPacket> MOB_ARMOR_EQUIPMENT_SERIALIZER = new MobArmorEquipmentSerializer_v712() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobArmorEquipmentPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<PlayerHotbarPacket> PLAYER_HOTBAR_SERIALIZER = new PlayerHotbarSerializer_v291() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, PlayerHotbarPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<PlayerSkinPacket> PLAYER_SKIN_SERIALIZER = new PlayerSkinSerializer_v390() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, PlayerSkinPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<SetEntityDataPacket> SET_ENTITY_DATA_SERIALIZER = new SetEntityDataSerializer_v557() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityDataPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<SetEntityMotionPacket> SET_ENTITY_MOTION_SERIALIZER = new SetEntityMotionSerializer_v662() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityMotionPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<SetEntityLinkPacket> SET_ENTITY_LINK_SERIALIZER = new SetEntityLinkSerializer_v291() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityLinkPacket packet) {
        }
    };


    private static final BedrockPacketSerializer<MobEquipmentPacket> MOB_EQUIPMENT_SERIALIZER = new MobEquipmentSerializer_v291() {
        override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobEquipmentPacket packet) {
            packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
            fakeItemRead(buffer);
            packet.setInventorySlot(buffer.readUnsignedByte());
            packet.setHotbarSlot(buffer.readUnsignedByte());
            packet.setContainerId(buffer.readByte());
        }
    };

    @SuppressWarnings("unchecked")
    static BedrockCodec processCodec(BedrockCodec codec) {
        BedrockPacketSerializer<BossEventPacket> bossEventSerializer;
        if (codec.getProtocolVersion() >= 776) {
            bossEventSerializer = BOSS_EVENT_SERIALIZER_776;
        } else {
            bossEventSerializer = BOSS_EVENT_SERIALIZER_486;
        }

        BedrockCodec.Builder codecBuilder = codec.toBuilder()

            .updateSerializer(PhotoTransferPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(LabTablePacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CodeBuilderSourcePacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CreatePhotoPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(NpcRequestPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(PhotoInfoRequestPacket.class, ILLEGAL_SERIALIZER)

            .updateSerializer(ClientCheatAbilityPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CraftingEventPacket.class, ILLEGAL_SERIALIZER)

            .updateSerializer(ClientCacheBlobStatusPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(SubClientLoginPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(SubChunkRequestPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(GameTestRequestPacket.class, ILLEGAL_SERIALIZER)

            .updateSerializer(ClientToServerHandshakePacket.class, IGNORED_SERIALIZER)
            .updateSerializer(EntityFallPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MapCreateLockedCopyPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MapInfoRequestPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(SettingsCommandPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(AnvilDamagePacket.class, IGNORED_SERIALIZER)

            .updateSerializer(InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V748)
            .updateSerializer(InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V748)
            .updateSerializer(MovePlayerPacket.class, MOVE_PLAYER_SERIALIZER)
            .updateSerializer(MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER)

            .updateSerializer(BossEventPacket.class, bossEventSerializer)
            .updateSerializer(MobArmorEquipmentPacket.class, MOB_ARMOR_EQUIPMENT_SERIALIZER)
            .updateSerializer(PlayerHotbarPacket.class, PLAYER_HOTBAR_SERIALIZER)
            .updateSerializer(PlayerSkinPacket.class, PLAYER_SKIN_SERIALIZER)
            .updateSerializer(SetEntityDataPacket.class, SET_ENTITY_DATA_SERIALIZER)
            .updateSerializer(SetEntityMotionPacket.class, SET_ENTITY_MOTION_SERIALIZER)
            .updateSerializer(SetEntityLinkPacket.class, SET_ENTITY_LINK_SERIALIZER)

            .updateSerializer(MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER)

            .updateSerializer(DebugInfoPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(EditorNetworkPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(ScriptMessagePacket.class, ILLEGAL_SERIALIZER)

            .updateSerializer(ClientCacheStatusPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(SimpleEventPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MultiplayerSettingsPacket.class, IGNORED_SERIALIZER);


            if (codec.getProtocolVersion() < 800) {
                codecBuilder
                    .updateSerializer(RiderJumpPacket.class, ILLEGAL_SERIALIZER)
                    .updateSerializer(PlayerInputPacket.class, ILLEGAL_SERIALIZER);
            }

            if (!Boolean.getBoolean("Geyser.ReceiptPackets")) {
                codecBuilder.updateSerializer(RefreshEntitlementsPacket.class, IGNORED_SERIALIZER);
                codecBuilder.updateSerializer(PurchaseReceiptPacket.class, IGNORED_SERIALIZER);
            }

            return codecBuilder.build();
    }


    private static void fakeItemRead(ByteBuf buffer) {
        int id = VarInts.readInt(buffer);
        if (id == 0) {
            return;
        }
        buffer.skipBytes(2);
        VarInts.readUnsignedInt(buffer);
        bool hasNetId = buffer.readBoolean();
        if (hasNetId) {
            VarInts.readInt(buffer);
        }

        VarInts.readInt(buffer);
        int streamSize = VarInts.readUnsignedInt(buffer);
        buffer.skipBytes(streamSize);
    }
}
