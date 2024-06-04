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

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobArmorEquipmentSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobEquipmentSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.PlayerHotbarSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityLinkSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityMotionSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v390.serializer.PlayerSkinSerializer_v390;
import org.cloudburstmc.protocol.bedrock.codec.v407.serializer.InventoryContentSerializer_v407;
import org.cloudburstmc.protocol.bedrock.codec.v407.serializer.InventorySlotSerializer_v407;
import org.cloudburstmc.protocol.bedrock.codec.v486.serializer.BossEventSerializer_v486;
import org.cloudburstmc.protocol.bedrock.codec.v557.serializer.SetEntityDataSerializer_v557;
import org.cloudburstmc.protocol.bedrock.codec.v662.serializer.SetEntityMotionSerializer_v662;
import org.cloudburstmc.protocol.bedrock.packet.AnvilDamagePacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BossEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheBlobStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCheatAbilityPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.CodeBuilderSourcePacket;
import org.cloudburstmc.protocol.bedrock.packet.CraftingEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.CreatePhotoPacket;
import org.cloudburstmc.protocol.bedrock.packet.DebugInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.EditorNetworkPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityFallPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameTestRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.LabTablePacket;
import org.cloudburstmc.protocol.bedrock.packet.MapCreateLockedCopyPacket;
import org.cloudburstmc.protocol.bedrock.packet.MapInfoRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MultiplayerSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.PhotoInfoRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.PhotoTransferPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;
import org.cloudburstmc.protocol.bedrock.packet.PurchaseReceiptPacket;
import org.cloudburstmc.protocol.bedrock.packet.RefreshEntitlementsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ScriptMessagePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SettingsCommandPacket;
import org.cloudburstmc.protocol.bedrock.packet.SimpleEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubChunkRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.TickSyncPacket;
import org.cloudburstmc.protocol.common.util.VarInts;

/**
 * Processes the Bedrock codec to remove or modify unused or unsafe packets and fields.
 */
class CodecProcessor {
    
    /**
     * Generic serializer that throws an exception when trying to serialize or deserialize a packet, leading to client disconnection.
     */
    @SuppressWarnings("rawtypes")
    private static final BedrockPacketSerializer ILLEGAL_SERIALIZER = new BedrockPacketSerializer<>() {
        @Override
        public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            throw new IllegalArgumentException("Server tried to send unused packet " + packet.getClass().getSimpleName() + "!");
        }

        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            throw new IllegalArgumentException("Client tried to send unused packet " + packet.getClass().getSimpleName() + "!");
        }
    };

    /**
     * Generic serializer that does nothing when trying to serialize or deserialize a packet.
     */
    @SuppressWarnings("rawtypes")
    private static final BedrockPacketSerializer IGNORED_SERIALIZER = new BedrockPacketSerializer<>() {
        @Override
        public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
        }

        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
        }
    };

    /**
     * Serializer that throws an exception when trying to deserialize InventoryContentPacket since server-auth inventory is used.
     */
    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER = new InventoryContentSerializer_v407() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalArgumentException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    /**
     * Serializer that throws an exception when trying to deserialize InventorySlotPacket since server-auth inventory is used.
     */
    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER = new InventorySlotSerializer_v407() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalArgumentException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize BossEventPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER = new BossEventSerializer_v486() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize MobArmorEquipmentPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<MobArmorEquipmentPacket> MOB_ARMOR_EQUIPMENT_SERIALIZER = new MobArmorEquipmentSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobArmorEquipmentPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize PlayerHotbarPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<PlayerHotbarPacket> PLAYER_HOTBAR_SERIALIZER = new PlayerHotbarSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, PlayerHotbarPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize PlayerSkinPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<PlayerSkinPacket> PLAYER_SKIN_SERIALIZER = new PlayerSkinSerializer_v390() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, PlayerSkinPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize SetEntityDataPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<SetEntityDataPacket> SET_ENTITY_DATA_SERIALIZER = new SetEntityDataSerializer_v557() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityDataPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize SetEntityMotionPacket since it is not used from the client for codec v291.
     */
    private static final BedrockPacketSerializer<SetEntityMotionPacket> SET_ENTITY_MOTION_SERIALIZER_V291 = new SetEntityMotionSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityMotionPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize SetEntityMotionPacket since it is not used from the client for codec v662.
     */
    private static final BedrockPacketSerializer<SetEntityMotionPacket> SET_ENTITY_MOTION_SERIALIZER_V662 = new SetEntityMotionSerializer_v662() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityMotionPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize SetEntityLinkPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<SetEntityLinkPacket> SET_ENTITY_LINK_SERIALIZER = new SetEntityLinkSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityLinkPacket packet) {
        }
    };

    /**
     * Serializer that skips over the item when trying to deserialize MobEquipmentPacket since only the slot info is used.
     */
    private static final BedrockPacketSerializer<MobEquipmentPacket> MOB_EQUIPMENT_SERIALIZER = new MobEquipmentSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobEquipmentPacket packet) {
            packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
            fakeItemRead(buffer);
            packet.setInventorySlot(buffer.readUnsignedByte());
            packet.setHotbarSlot(buffer.readUnsignedByte());
            packet.setContainerId(buffer.readByte());
        }
    };

    @SuppressWarnings("unchecked")
    static BedrockCodec processCodec(BedrockCodec codec) {
        return codec.toBuilder()
            // Illegal unused serverbound EDU packets
            .updateSerializer(PhotoTransferPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(LabTablePacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CodeBuilderSourcePacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CreatePhotoPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(NpcRequestPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(PhotoInfoRequestPacket.class, ILLEGAL_SERIALIZER)
            // Unused serverbound packets for featured servers, which is for some reason still occasionally sent
            .updateSerializer(PurchaseReceiptPacket.class, IGNORED_SERIALIZER)
            // Illegal unused serverbound packets that are deprecated
            .updateSerializer(ClientCheatAbilityPacket.class, ILLEGAL_SERIALIZER)
            // Illegal unusued serverbound packets that relate to unused features
            .updateSerializer(PlayerAuthInputPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(ClientCacheBlobStatusPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(SubClientLoginPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(SubChunkRequestPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(GameTestRequestPacket.class, ILLEGAL_SERIALIZER)
            // Ignored serverbound packets
            .updateSerializer(CraftingEventPacket.class, IGNORED_SERIALIZER) // Make illegal when 1.20.40 is removed
            .updateSerializer(ClientToServerHandshakePacket.class, IGNORED_SERIALIZER)
            .updateSerializer(EntityFallPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MapCreateLockedCopyPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MapInfoRequestPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(SettingsCommandPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(AnvilDamagePacket.class, IGNORED_SERIALIZER)
            .updateSerializer(RefreshEntitlementsPacket.class, IGNORED_SERIALIZER)
            // Illegal when serverbound due to Geyser specific setup
            .updateSerializer(InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER)
            .updateSerializer(InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER)
            // Ignored only when serverbound
            .updateSerializer(BossEventPacket.class, BOSS_EVENT_SERIALIZER)
            .updateSerializer(MobArmorEquipmentPacket.class, MOB_ARMOR_EQUIPMENT_SERIALIZER)
            .updateSerializer(PlayerHotbarPacket.class, PLAYER_HOTBAR_SERIALIZER)
            .updateSerializer(PlayerSkinPacket.class, PLAYER_SKIN_SERIALIZER)
            .updateSerializer(SetEntityDataPacket.class, SET_ENTITY_DATA_SERIALIZER)
            .updateSerializer(SetEntityMotionPacket.class, codec.getProtocolVersion() < 662 ?
                SET_ENTITY_MOTION_SERIALIZER_V291 : 
                SET_ENTITY_MOTION_SERIALIZER_V662)
            .updateSerializer(SetEntityLinkPacket.class, SET_ENTITY_LINK_SERIALIZER)
            // Valid serverbound packets where reading of some fields can be skipped
            .updateSerializer(MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER)
            // // Illegal bidirectional packets
            .updateSerializer(DebugInfoPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(EditorNetworkPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(ScriptMessagePacket.class, ILLEGAL_SERIALIZER)
            // // Ignored bidirectional packets
            .updateSerializer(ClientCacheStatusPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(SimpleEventPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(TickSyncPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MultiplayerSettingsPacket.class, IGNORED_SERIALIZER)
            .build();
    }

    /**
     * Fake reading an item from the buffer to improve performance.
     * 
     * @param buffer
     */
    private static void fakeItemRead(ByteBuf buffer) {
        int id = VarInts.readInt(buffer); // Runtime ID
        if (id == 0) { // nothing more to read
            return;
        }
        buffer.skipBytes(2); // count
        VarInts.readUnsignedInt(buffer); // damage
        boolean hasNetId = buffer.readBoolean();
        if (hasNetId) {
            VarInts.readInt(buffer);
        }

        VarInts.readInt(buffer); // Block runtime ID
        int streamSize = VarInts.readUnsignedInt(buffer);
        buffer.skipBytes(streamSize);
    }
}
