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
import org.cloudburstmc.protocol.bedrock.codec.v1001.serializer.BossEventSerializer_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v1001.serializer.InventoryContentSerializer_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v1001.serializer.MobArmorEquipmentSerializer_v1001;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobArmorEquipmentSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobEquipmentSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MoveEntityAbsoluteSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.PlayerHotbarSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityLinkSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityMotionSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v390.serializer.PlayerSkinSerializer_v390;
import org.cloudburstmc.protocol.bedrock.codec.v407.serializer.InventoryContentSerializer_v407;
import org.cloudburstmc.protocol.bedrock.codec.v407.serializer.InventorySlotSerializer_v407;
import org.cloudburstmc.protocol.bedrock.codec.v557.serializer.SetEntityDataSerializer_v557;
import org.cloudburstmc.protocol.bedrock.codec.v662.serializer.SetEntityMotionSerializer_v662;
import org.cloudburstmc.protocol.bedrock.codec.v712.serializer.InventoryContentSerializer_v712;
import org.cloudburstmc.protocol.bedrock.codec.v712.serializer.InventorySlotSerializer_v712;
import org.cloudburstmc.protocol.bedrock.codec.v712.serializer.MobArmorEquipmentSerializer_v712;
import org.cloudburstmc.protocol.bedrock.codec.v729.serializer.InventoryContentSerializer_v729;
import org.cloudburstmc.protocol.bedrock.codec.v729.serializer.InventorySlotSerializer_v729;
import org.cloudburstmc.protocol.bedrock.codec.v748.serializer.InventoryContentSerializer_v748;
import org.cloudburstmc.protocol.bedrock.codec.v748.serializer.InventorySlotSerializer_v748;
import org.cloudburstmc.protocol.bedrock.codec.v776.serializer.BossEventSerializer_v776;
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
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SettingsCommandPacket;
import org.cloudburstmc.protocol.bedrock.packet.SimpleEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubChunkRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.geysermc.geyser.network.netty.IllegalPacketException;

/**
 * Processes the Bedrock codec to remove or modify unused or unsafe packets and fields.
 */
@SuppressWarnings("deprecation")
class CodecProcessor {
    
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
     * Inventory item wire format changed at 407 → 712 → 729 → 748 → 1001; using the wrong era crashes the Bedrock client
     * ("server sent a broken packet").
     */
    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER_V407 = new InventoryContentSerializer_v407() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalPacketException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER_V712 = new InventoryContentSerializer_v712() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalPacketException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER_V729 = new InventoryContentSerializer_v729() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalPacketException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER_V748 = new InventoryContentSerializer_v748() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalPacketException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventoryContentPacket> INVENTORY_CONTENT_SERIALIZER_V1001 = new InventoryContentSerializer_v1001() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
            throw new IllegalPacketException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
        }
    };

    /*
     * Serializer that throws an exception when trying to deserialize InventorySlotPacket since server-auth inventory is used.
     */
    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER_V407 = new InventorySlotSerializer_v407() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalPacketException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER_V712 = new InventorySlotSerializer_v712() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalPacketException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER_V729 = new InventorySlotSerializer_v729() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalPacketException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER_V748 = new InventorySlotSerializer_v748() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalPacketException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    private static final BedrockPacketSerializer<InventorySlotPacket> INVENTORY_SLOT_SERIALIZER_V975 = new InventorySlotSerializer_v975() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
            throw new IllegalPacketException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
        }
    };

    // Intentionally do NOT ban inbound MovePlayerPacket. With SERVER-auth movement, modern clients
    // use PlayerAuthInput — but legacy 1.20.x still emits MovePlayer during spawn/join. Throwing in
    // deserialize kicks them ("Invalid packet received!") even though StartGame uses SERVER mode.

    private static final BedrockPacketSerializer<MoveEntityAbsolutePacket> MOVE_ENTITY_SERIALIZER_V291 = new MoveEntityAbsoluteSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MoveEntityAbsolutePacket packet) {
            throw new IllegalPacketException("Client cannot send MoveEntityAbsolutePacket in server-auth movement environment!");
        }
    };

    private static final BedrockPacketSerializer<MoveEntityAbsolutePacket> MOVE_ENTITY_SERIALIZER_V975 = new MoveEntityAbsoluteSerializer_v975() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MoveEntityAbsolutePacket packet) {
            throw new IllegalPacketException("Client cannot send MoveEntityAbsolutePacket in server-auth movement environment!");
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize BossEventPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER_V776 = new BossEventSerializer_v776() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };

    private static final BedrockPacketSerializer<BossEventPacket> BOSS_EVENT_SERIALIZER_V1001 = new BossEventSerializer_v1001() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize MobArmorEquipmentPacket since it is not used from the client.
     */
    private static final BedrockPacketSerializer<MobArmorEquipmentPacket> MOB_ARMOR_EQUIPMENT_SERIALIZER_V291 = new MobArmorEquipmentSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobArmorEquipmentPacket packet) {
        }
    };

    private static final BedrockPacketSerializer<MobArmorEquipmentPacket> MOB_ARMOR_EQUIPMENT_SERIALIZER_V712 = new MobArmorEquipmentSerializer_v712() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobArmorEquipmentPacket packet) {
        }
    };

    private static final BedrockPacketSerializer<MobArmorEquipmentPacket> MOB_ARMOR_EQUIPMENT_SERIALIZER_V1001 = new MobArmorEquipmentSerializer_v1001() {
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
     * Pre-1.20.70 SetEntityMotion (no tick field). Forcing the v662 serializer on 1.20.60/62
     * crashes the client with "broken packet" — restored after fa6808a62 dropped &lt;1.20.80.
     */
    private static final BedrockPacketSerializer<SetEntityMotionPacket> SET_ENTITY_MOTION_SERIALIZER_V291 = new SetEntityMotionSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityMotionPacket packet) {
        }
    };

    /**
     * Serializer that does nothing when trying to deserialize SetEntityMotionPacket since it is not used from the client.
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
    private static final BedrockPacketSerializer<MobEquipmentPacket> MOB_EQUIPMENT_SERIALIZER_V291 = new MobEquipmentSerializer_v291() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobEquipmentPacket packet) {
            packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
            fakeItemRead(buffer);
            packet.setInventorySlot(buffer.readUnsignedByte());
            packet.setHotbarSlot(buffer.readUnsignedByte());
            packet.setContainerId(buffer.readByte());
        }
    };

    private static final BedrockPacketSerializer<MobEquipmentPacket> MOB_EQUIPMENT_SERIALIZER_V975 = new MobEquipmentSerializer_v975() {
        @Override
        public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobEquipmentPacket packet) {
            packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
            fakeItemDescriptorRead(buffer);
            packet.setInventorySlot(buffer.readUnsignedByte());
            packet.setHotbarSlot(buffer.readUnsignedByte());
            packet.setContainerId(buffer.readByte());
        }
    };

    @SuppressWarnings("unchecked")
    static BedrockCodec processCodec(BedrockCodec codec) {
        BedrockCodec.Builder codecBuilder = codec.toBuilder();

        // Illegal unused serverbound EDU packets
        updateSerializerIfPresent(codecBuilder, PhotoTransferPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, LabTablePacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, CodeBuilderSourcePacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, CreatePhotoPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, NpcRequestPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, PhotoInfoRequestPacket.class, ILLEGAL_SERIALIZER);
        // Illegal unused serverbound packets that are deprecated
        updateSerializerIfPresent(codecBuilder, ClientCheatAbilityPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, CraftingEventPacket.class, ILLEGAL_SERIALIZER);
        // Illegal unused serverbound packets that relate to unused features
        updateSerializerIfPresent(codecBuilder, ClientCacheBlobStatusPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, SubClientLoginPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, SubChunkRequestPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, GameTestRequestPacket.class, ILLEGAL_SERIALIZER);
        // Illegal bidirectional packets
        updateSerializerIfPresent(codecBuilder, DebugInfoPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, EditorNetworkPacket.class, ILLEGAL_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, ScriptMessagePacket.class, ILLEGAL_SERIALIZER);

        // Ignored serverbound packets
        updateSerializerIfPresent(codecBuilder, ClientToServerHandshakePacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, EntityFallPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, MapCreateLockedCopyPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, MapInfoRequestPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, SettingsCommandPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, AnvilDamagePacket.class, IGNORED_SERIALIZER);
        // Ignored bidirectional packets
        updateSerializerIfPresent(codecBuilder, ClientCacheStatusPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, SimpleEventPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, MultiplayerSettingsPacket.class, IGNORED_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, EmoteListPacket.class, IGNORED_SERIALIZER);
        // Ignored only when serverbound (MovePlayer kept as stock codec — legacy 1.20.x still sends it)
        updateSerializerIfPresent(codecBuilder, PlayerHotbarPacket.class, PLAYER_HOTBAR_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, PlayerSkinPacket.class, PLAYER_SKIN_SERIALIZER);
        updateSerializerIfPresent(codecBuilder, SetEntityDataPacket.class, SET_ENTITY_DATA_SERIALIZER);
        // 1.20.70 (662) added a tick field to SetEntityMotion; older clients must keep v291 wire format.
        if (codec.getProtocolVersion() < 662) {
            updateSerializerIfPresent(codecBuilder, SetEntityMotionPacket.class, SET_ENTITY_MOTION_SERIALIZER_V291);
        } else {
            updateSerializerIfPresent(codecBuilder, SetEntityMotionPacket.class, SET_ENTITY_MOTION_SERIALIZER_V662);
        }
        updateSerializerIfPresent(codecBuilder, SetEntityLinkPacket.class, SET_ENTITY_LINK_SERIALIZER);

        if (!Boolean.getBoolean("Geyser.ReceiptPackets")) {
            // RefreshEntitlements / PurchaseReceipt are missing on older 1.20.x codecs
            updateSerializerIfPresent(codecBuilder, RefreshEntitlementsPacket.class, IGNORED_SERIALIZER);
            updateSerializerIfPresent(codecBuilder, PurchaseReceiptPacket.class, IGNORED_SERIALIZER);
            // Added in 26.10, doesn't exist on 26.0
            if (codec.getProtocolVersion() >= 944) {
                updateSerializerIfPresent(codecBuilder, PartyChangedPacket.class, IGNORED_SERIALIZER);
            }
        }

        if (codec.getProtocolVersion() < 712) { // 1.21.2 / 1.21.0 and below — pre-FullContainerName inventory
            updateSerializerIfPresent(codecBuilder, InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V407);
            updateSerializerIfPresent(codecBuilder, InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V407);
            updateSerializerIfPresent(codecBuilder, MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER_V291);
            updateSerializerIfPresent(codecBuilder, MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER_V291);
        } else if (codec.getProtocolVersion() < 729) { // 1.21.20
            // Bedrock_v712 added FullContainerName / dynamicId to inventory packets.
            updateSerializerIfPresent(codecBuilder, InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V712);
            updateSerializerIfPresent(codecBuilder, InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V712);
            updateSerializerIfPresent(codecBuilder, MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER_V291);
            updateSerializerIfPresent(codecBuilder, MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER_V291);
        } else if (codec.getProtocolVersion() < 748) { // 1.21.30
            updateSerializerIfPresent(codecBuilder, InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V729);
            updateSerializerIfPresent(codecBuilder, InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V729);
            updateSerializerIfPresent(codecBuilder, MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER_V291);
            updateSerializerIfPresent(codecBuilder, MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER_V291);
        } else if (codec.getProtocolVersion() < 975) { // 1.21.40 through pre-26.20
            updateSerializerIfPresent(codecBuilder, InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V748);
            updateSerializerIfPresent(codecBuilder, InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V748);
            updateSerializerIfPresent(codecBuilder, MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER_V291);
            updateSerializerIfPresent(codecBuilder, MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER_V291);
        } else if (codec.getProtocolVersion() < 1001) { // 26.20 / 26.21…
            updateSerializerIfPresent(codecBuilder, InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V975);
            updateSerializerIfPresent(codecBuilder, InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V748);
            updateSerializerIfPresent(codecBuilder, MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER_V975);
            updateSerializerIfPresent(codecBuilder, MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER_V975);
        } else {
            updateSerializerIfPresent(codecBuilder, InventorySlotPacket.class, INVENTORY_SLOT_SERIALIZER_V975);
            updateSerializerIfPresent(codecBuilder, InventoryContentPacket.class, INVENTORY_CONTENT_SERIALIZER_V1001);
            updateSerializerIfPresent(codecBuilder, MoveEntityAbsolutePacket.class, MOVE_ENTITY_SERIALIZER_V975);
            updateSerializerIfPresent(codecBuilder, MobEquipmentPacket.class, MOB_EQUIPMENT_SERIALIZER_V975);
        }

        if (codec.getProtocolVersion() < 712) { // pre-body-slot armor
            updateSerializerIfPresent(codecBuilder, MobArmorEquipmentPacket.class, MOB_ARMOR_EQUIPMENT_SERIALIZER_V291);
            updateSerializerIfPresent(codecBuilder, BossEventPacket.class, BOSS_EVENT_SERIALIZER_V776);
        } else if (codec.getProtocolVersion() < 1001) { // 26.30
            updateSerializerIfPresent(codecBuilder, MobArmorEquipmentPacket.class, MOB_ARMOR_EQUIPMENT_SERIALIZER_V712);
            updateSerializerIfPresent(codecBuilder, BossEventPacket.class, BOSS_EVENT_SERIALIZER_V776);
        } else {
            updateSerializerIfPresent(codecBuilder, MobArmorEquipmentPacket.class, MOB_ARMOR_EQUIPMENT_SERIALIZER_V1001);
            updateSerializerIfPresent(codecBuilder, BossEventPacket.class, BOSS_EVENT_SERIALIZER_V1001);
        }

        return codecBuilder.build();
    }

    /**
     * Older Bedrock codecs omit packets that exist on newer versions (and vice versa).
     * Skip serializer overrides when the packet is not part of this codec.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void updateSerializerIfPresent(BedrockCodec.Builder builder,
                                                  Class<? extends BedrockPacket> packetClass,
                                                  BedrockPacketSerializer serializer) {
        try {
            builder.updateSerializer(packetClass, serializer);
        } catch (IllegalArgumentException ignored) {
            // Packet does not exist in this protocol version
        }
    }

    /**
     * Fake reading an item from the buffer to improve performance.
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

    /**
     * Fake reading an item descriptor from the buffer to improve performance.
     * Used after 26.20; apparently... yippie
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
