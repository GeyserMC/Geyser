/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network.bedrock;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.codec.v1001.serializer.BossEventSerializer_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v1001.serializer.InventoryContentSerializer_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v1001.serializer.MobArmorEquipmentSerializer_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v2168.Bedrock_v2168;
import org.cloudburstmc.protocol.bedrock.codec.v2168.serializer.MovePlayerSerializer_v2168;
import org.cloudburstmc.protocol.bedrock.codec.v2168.serializer.PlayerSkinSerializer_v2168;
import org.cloudburstmc.protocol.bedrock.codec.v2193.serializer.BossEventSerializer_v2193;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.PlayerHotbarSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityLinkSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v557.serializer.SetEntityDataSerializer_v557;
import org.cloudburstmc.protocol.bedrock.codec.v662.serializer.SetEntityMotionSerializer_v662;
import org.cloudburstmc.protocol.bedrock.codec.v975.serializer.InventorySlotSerializer_v975;
import org.cloudburstmc.protocol.bedrock.codec.v975.serializer.MobEquipmentSerializer_v975;
import org.cloudburstmc.protocol.bedrock.codec.v975.serializer.MoveEntityAbsoluteSerializer_v975;
import org.cloudburstmc.protocol.bedrock.packet.AnvilDamagePacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BossEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheBlobStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCheatAbilityPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundMatchmakingStatePacket;
import org.cloudburstmc.protocol.bedrock.packet.CodeBuilderSourcePacket;
import org.cloudburstmc.protocol.bedrock.packet.CraftingEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.CreatePhotoPacket;
import org.cloudburstmc.protocol.bedrock.packet.DebugInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.EditorNetworkPacket;
import org.cloudburstmc.protocol.bedrock.packet.EmoteListPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityFallPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameTestRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.protocol.bedrock.packet.LabTablePacket;
import org.cloudburstmc.protocol.bedrock.packet.MapCreateLockedCopyPacket;
import org.cloudburstmc.protocol.bedrock.packet.MapInfoRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MultiplayerSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.PartyChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.PhotoInfoRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.PhotoTransferPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;
import org.cloudburstmc.protocol.bedrock.packet.PurchaseReceiptPacket;
import org.cloudburstmc.protocol.bedrock.packet.RefreshEntitlementsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ScriptMessagePacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerboundMatchmakingCancelPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SettingsCommandPacket;
import org.cloudburstmc.protocol.bedrock.packet.SimpleEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubChunkRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.UpstreamSession;

/**
 * Processes the Bedrock codec to remove or modify unused or unsafe packets and fields.
 */
@SuppressWarnings("deprecation")
public class CodecProcessor {
    
    /**
     * Generic serializer that throws an exception when trying to serialize or deserialize a packet, leading to client disconnection.
     */
    @SuppressWarnings("rawtypes")
    static final BedrockPacketSerializer ILLEGAL_SERIALIZER = new BedrockPacketSerializer<>() {
        @Override
        public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            throw new IllegalPacketException("Server tried to send unused packet " + packet.getClass().getSimpleName() + "!");
        }

        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            throw new IllegalPacketException("Client tried to send unused packet " + packet.getClass().getSimpleName() + "!");
        }
    };

    /**
     * Generic serializer that does nothing when trying to serialize or deserialize a packet.
     */
    @SuppressWarnings("rawtypes")
    static final BedrockPacketSerializer IGNORED_SERIALIZER = new BedrockPacketSerializer<>() {
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
    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER = new InventoryContentSerializer_v1001() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalPacketException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    /**
     * Serializer that throws an exception when trying to deserialize InventorySlotPacket since server-auth inventory is used.
     */
    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER = new InventorySlotSerializer_v975() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalPacketException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    /**
     * Serializer that throws an exception when trying to deserialize MovePlayerPacket since server-auth movement is used.
     */
    private static final BedrockPacketSerializer<MovePlayerPacket> MOVE_PLAYER_SERIALIZER = new MovePlayerSerializer_v2168() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MovePlayerPacket packet) {
            throw new IllegalPacketException("Client cannot send MovePlayerPacket in server-auth movement environment!");
        }
    };

    /**
     * Serializer that throws an exception when trying to deserialize MoveEntityAbsolutePacket since server-auth movement is used.
     */
    private static final BedrockPacketSerializer<MoveEntityAbsolutePacket> MOVE_ENTITY_SERIALIZER = new MoveEntityAbsoluteSerializer_v975() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MoveEntityAbsolutePacket packet) {
            throw new IllegalPacketException("Client cannot send MoveEntityAbsolutePacket in server-auth movement environment!");
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize BossEventPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER_V1001 = new BossEventSerializer_v1001() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };

    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER_V2193 = new BossEventSerializer_v2193() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize MobArmorEquipmentPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<MobArmorEquipmentPacket> MOB_ARMOR_EQUIPMENT_SERIALIZER = new MobArmorEquipmentSerializer_v1001() {
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
    private static final BedrockPacketSerializer<PlayerSkinPacket> PLAYER_SKIN_SERIALIZER = new PlayerSkinSerializer_v2168() {
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
     * Serializer that does nothing when trying to deserialize SetEntityMotionPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<SetEntityMotionPacket> SET_ENTITY_MOTION_SERIALIZER = new SetEntityMotionSerializer_v662() {
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
    private static final BedrockPacketSerializer<MobEquipmentPacket> MOB_EQUIPMENT_SERIALIZER = new MobEquipmentSerializer_v975() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobEquipmentPacket packet) {
            packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
            fakeItemDescriptorRead(buffer);
            packet.setInventorySlot(buffer.readUnsignedByte());
            packet.setHotbarSlot(buffer.readUnsignedByte());
            packet.setContainerId(buffer.readByte());
        }
    };

    public static void updateCodec(UpstreamSession upstreamSession, String version) {
        // Support all older 1.26.4x builds
        switch (version) {
            case "1.26.40", "1.26.41", "1.26.42", "1.26.43" -> upstreamSession.getSession().setCodec(processCodec(Bedrock_v2168.CODEC));
        }
    }

    @SuppressWarnings("unchecked")
    static BedrockCodec processCodec(BedrockCodec codec) {
        BedrockCodec.Builder codecBuilder = codec.toBuilder()
            // Illegal unused serverbound EDU packets
            .updateSerializer(PhotoTransferPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(LabTablePacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CodeBuilderSourcePacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CreatePhotoPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(NpcRequestPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(PhotoInfoRequestPacket.class, ILLEGAL_SERIALIZER)
            // Illegal unused serverbound packets that are deprecated
            .updateSerializer(ClientCheatAbilityPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(CraftingEventPacket.class, ILLEGAL_SERIALIZER)
            // Illegal unusued serverbound packets that relate to unused features
            .updateSerializer(ClientCacheBlobStatusPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(SubChunkRequestPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(GameTestRequestPacket.class, ILLEGAL_SERIALIZER)
            // Illegal bidirectional packets
            .updateSerializer(DebugInfoPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(EditorNetworkPacket.class, ILLEGAL_SERIALIZER)
            .updateSerializer(ScriptMessagePacket.class, ILLEGAL_SERIALIZER)

            // Ignored serverbound packets
            .updateSerializer(ClientToServerHandshakePacket.class, IGNORED_SERIALIZER)
            .updateSerializer(EntityFallPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MapCreateLockedCopyPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MapInfoRequestPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(SettingsCommandPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(AnvilDamagePacket.class, IGNORED_SERIALIZER)
            // Ignored bidirectional packets
            .updateSerializer(ClientCacheStatusPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(SimpleEventPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(MultiplayerSettingsPacket.class, IGNORED_SERIALIZER)
            .updateSerializer(EmoteListPacket.class, IGNORED_SERIALIZER)
            // Ignored only when serverbound
            .updateSerializer(PlayerHotbarPacket.class, PLAYER_HOTBAR_SERIALIZER)
            .updateSerializer(SetEntityDataPacket.class, SET_ENTITY_DATA_SERIALIZER)
            .updateSerializer(SetEntityMotionPacket.class, SET_ENTITY_MOTION_SERIALIZER)
            .updateSerializer(SetEntityLinkPacket.class, SET_ENTITY_LINK_SERIALIZER)
            .updateSerializer(MobArmorEquipmentPacket.class, MOB_ARMOR_EQUIPMENT_SERIALIZER)
            .updateSerializer(PlayerSkinPacket.class, PLAYER_SKIN_SERIALIZER)
            // Illegal only when serverbound
            .updateSerializer(InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER)
            .updateSerializer(InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER)
            .updateSerializer(MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER)
            .updateSerializer(MovePlayerPacket.class, MOVE_PLAYER_SERIALIZER)
            // Skips over some unused fields
            .updateSerializer(MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER);

        boolean legacyReceiptPackets = Boolean.getBoolean("Geyser.ReceiptPackets");
        if (legacyReceiptPackets) {
            GeyserImpl.getInstance().getLogger().warning("You're using `-DGeyser.ReceiptPackets=true`," +
                " this is now deprecated. Please switch to using `-DGeyser.PartneredServerPackets=true`.");
        }

        if (!legacyReceiptPackets && !Boolean.getBoolean("Geyser.PartneredServerPackets")) {
            codecBuilder.updateSerializer(RefreshEntitlementsPacket.class, IGNORED_SERIALIZER);
            codecBuilder.updateSerializer(PurchaseReceiptPacket.class, IGNORED_SERIALIZER);
            codecBuilder.updateSerializer(PartyChangedPacket.class, IGNORED_SERIALIZER);

            // Added in 26.60
            if (GameProtocol.is26_60orHigher(codec.getProtocolVersion())) {
                codecBuilder.updateSerializer(ClientboundMatchmakingStatePacket.class, IGNORED_SERIALIZER);
                codecBuilder.updateSerializer(ServerboundMatchmakingCancelPacket.class, IGNORED_SERIALIZER);
            }
        }

        if (GameProtocol.is26_50orHigher(codec.getProtocolVersion())) {
            codecBuilder.updateSerializer(BossEventPacket.class, BOSS_EVENT_SERIALIZER_V2193);
        } else {
            codecBuilder.updateSerializer(BossEventPacket.class, BOSS_EVENT_SERIALIZER_V1001);
        }

        return codecBuilder.build();
    }

    /**
     * Fake reading an item descriptor from the buffer to improve performance.
     */
    private static void fakeItemDescriptorRead(ByteBuf buffer) {
        buffer.readShortLE(); // runtimeId
        buffer.readUnsignedShortLE(); // count
        VarInts.readUnsignedInt(buffer); // damage / aux
        boolean hasNetId = buffer.readBoolean();

        if (hasNetId) {
            int netIdVariant = VarInts.readUnsignedInt(buffer);
            switch (netIdVariant) {
                case 0: // ItemStackNetId
                case 1: // ItemStackRequestId
                case 2: // ItemStackLegacyRequestId
                    VarInts.readInt(buffer); // netId
                    break;
                default:
                    throw new IllegalPacketException("Not oneOf<ItemStackNetId, ItemStackRequestId, ItemStackLegacyRequestId>");
            }
        }
        VarInts.readUnsignedInt(buffer); // block runtime id
        int streamSize = VarInts.readUnsignedInt(buffer);
        buffer.skipBytes(streamSize);
    }
}
