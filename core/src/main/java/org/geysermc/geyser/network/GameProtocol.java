/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.PacketCodec;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketSerializer;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobArmorEquipmentSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.MobEquipmentSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.PlayerHotbarSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.SetEntityLinkSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v390.serializer.PlayerSkinSerializer_v390;
import org.cloudburstmc.protocol.bedrock.codec.v407.serializer.InventoryContentSerializer_v407;
import org.cloudburstmc.protocol.bedrock.codec.v407.serializer.InventorySlotSerializer_v407;
import org.cloudburstmc.protocol.bedrock.codec.v486.serializer.BossEventSerializer_v486;
import org.cloudburstmc.protocol.bedrock.codec.v557.serializer.SetEntityDataSerializer_v557;
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622;
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630;
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.cloudburstmc.protocol.bedrock.codec.v662.serializer.SetEntityMotionSerializer_v662;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.netty.codec.packet.BedrockPacketCodec;
import org.cloudburstmc.protocol.bedrock.packet.AnvilDamagePacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BossEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheBlobStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientCheatAbilityPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.CraftingEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.CreatePhotoPacket;
import org.cloudburstmc.protocol.bedrock.packet.DebugInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket;
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
import org.cloudburstmc.protocol.bedrock.packet.ScriptCustomEventPacket;
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
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Contains information about the supported protocols in Geyser.
 */
public final class GameProtocol {
    /**
     * Default Bedrock codec that should act as a fallback. Should represent the latest available
     * release of the game that Geyser supports.
     */
    public static final BedrockCodec DEFAULT_BEDROCK_CODEC = processCodec(Bedrock_v671.CODEC);

    /**
     * A list of all supported Bedrock versions that can join Geyser
     */
    public static final List<BedrockCodec> SUPPORTED_BEDROCK_CODECS = new ArrayList<>();

    /**
     * Java codec that is supported. We only ever support one version for
     * Java Edition.
     */
    private static final PacketCodec DEFAULT_JAVA_CODEC = MinecraftCodec.CODEC;

    static {
        SUPPORTED_BEDROCK_CODECS.add(processCodec(Bedrock_v622.CODEC.toBuilder()
            .minecraftVersion("1.20.40/1.20.41")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(processCodec(Bedrock_v630.CODEC.toBuilder()
            .minecraftVersion("1.20.50/1.20.51")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(processCodec(Bedrock_v649.CODEC.toBuilder()
            .minecraftVersion("1.20.60/1.20.62")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(processCodec(Bedrock_v662.CODEC.toBuilder()
            .minecraftVersion("1.20.70/1.20.73")
            .build()));
        SUPPORTED_BEDROCK_CODECS.add(processCodec(DEFAULT_BEDROCK_CODEC.toBuilder()
            .minecraftVersion("1.20.80")
            .build()));
    }

    /**
     * Gets the {@link BedrockPacketCodec} of the given protocol version.
     * @param protocolVersion The protocol version to attempt to find
     * @return The packet codec, or null if the client's protocol is unsupported
     */
    public static @Nullable BedrockCodec getBedrockCodec(int protocolVersion) {
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            if (packetCodec.getProtocolVersion() == protocolVersion) {
                return packetCodec;
            }
        }
        return null;
    }

    /* Bedrock convenience methods to gatekeep features and easily remove the check on version removal */

    public static boolean isPre1_20_50(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v630.CODEC.getProtocolVersion();
    }

    public static boolean isPre1_20_70(GeyserSession session) {
        return session.getUpstream().getProtocolVersion() < Bedrock_v662.CODEC.getProtocolVersion();
    }

    public static boolean is1_20_60orHigher(int protocolVersion) {
        return protocolVersion >= Bedrock_v649.CODEC.getProtocolVersion();
    }

    /**
     * Gets the {@link PacketCodec} for Minecraft: Java Edition.
     *
     * @return the packet codec for Minecraft: Java Edition
     */
    public static PacketCodec getJavaCodec() {
        return DEFAULT_JAVA_CODEC;
    }

    /**
     * Gets the supported Minecraft: Java Edition version names.
     *
     * @return the supported Minecraft: Java Edition version names
     */
    public static List<String> getJavaVersions() {
        return List.of(DEFAULT_JAVA_CODEC.getMinecraftVersion());
    }

    /**
     * Gets the supported Minecraft: Java Edition protocol version.
     *
     * @return the supported Minecraft: Java Edition protocol version
     */
    public static int getJavaProtocolVersion() {
        return DEFAULT_JAVA_CODEC.getProtocolVersion();
    }

    /**
     * Gets the supported Minecraft: Java Edition version.
     *
     * @return the supported Minecraft: Java Edition version
     */
    public static String getJavaMinecraftVersion() {
        return DEFAULT_JAVA_CODEC.getMinecraftVersion();
    }

    /**
     * @return a string showing all supported Bedrock versions for this Geyser instance
     */
    public static String getAllSupportedBedrockVersions() {
        StringJoiner joiner = new StringJoiner(", ");
        for (BedrockCodec packetCodec : SUPPORTED_BEDROCK_CODECS) {
            joiner.add(packetCodec.getMinecraftVersion());
        }

        return joiner.toString();
    }

    /**
     * @return a string showing all supported Java versions for this Geyser instance
     */
    public static String getAllSupportedJavaVersions() {
        StringJoiner joiner = new StringJoiner(", ");
        for (String version : getJavaVersions()) {
            joiner.add(version);
        }

        return joiner.toString();
    }

    private static BedrockCodec processCodec(BedrockCodec codec) {
        return codec.toBuilder()
            // Illegal unused serverbound EDU packets
            .updateSerializer(PhotoTransferPacket.class, setIllegalSerializer())
            .updateSerializer(LabTablePacket.class, setIllegalSerializer())
            .updateSerializer(CreatePhotoPacket.class, setIllegalSerializer())
            .updateSerializer(NpcRequestPacket.class, setIllegalSerializer())
            .updateSerializer(PhotoInfoRequestPacket.class, setIllegalSerializer())
            // Illegal unused serverbound packets for featured servers
            .updateSerializer(PurchaseReceiptPacket.class, setIllegalSerializer())
            // Illegal unused serverbound packets that are deprecated
            .updateSerializer(ClientCheatAbilityPacket.class, setIllegalSerializer())
            // Illegal unusued serverbound packets that relate to unused features
            .updateSerializer(PlayerAuthInputPacket.class, setIllegalSerializer())
            .updateSerializer(ClientCacheBlobStatusPacket.class, setIllegalSerializer())
            .updateSerializer(ClientCacheStatusPacket.class, setIllegalSerializer())
            .updateSerializer(SubClientLoginPacket.class, setIllegalSerializer())
            .updateSerializer(SubChunkRequestPacket.class, setIllegalSerializer())
            .updateSerializer(GameTestRequestPacket.class, setIllegalSerializer())
            // Ignored serverbound packets
            .updateSerializer(CraftingEventPacket.class, setIgnoredSerializer()) // Make illegal when 1.20.40 is removed
            .updateSerializer(ClientToServerHandshakePacket.class, setIgnoredSerializer())
            .updateSerializer(EntityFallPacket.class, setIgnoredSerializer())
            .updateSerializer(MapCreateLockedCopyPacket.class, setIgnoredSerializer())
            .updateSerializer(MapInfoRequestPacket.class, setIgnoredSerializer())
            .updateSerializer(SettingsCommandPacket.class, setIgnoredSerializer())
            .updateSerializer(AnvilDamagePacket.class, setIgnoredSerializer())
            // Illegal when serverbound due to Geyser specific setup
            .updateSerializer(InventoryContentPacket.class, new InventoryContentSerializer_v407() {
                @Override
                public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventoryContentPacket packet) {
                    throw new IllegalArgumentException("Client cannot send InventoryContentPacket in server-auth inventory environment!");
                }
            })
            .updateSerializer(InventorySlotPacket.class, new InventorySlotSerializer_v407() {
                @Override
                public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, InventorySlotPacket packet) {
                    throw new IllegalArgumentException("Client cannot send InventorySlotPacket in server-auth inventory environment!");
                }
            })
            // Ignored only when serverbound
            .updateSerializer(BossEventPacket.class, new BossEventSerializer_v486() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BossEventPacket packet) {}
            })
            .updateSerializer(MobArmorEquipmentPacket.class, new MobArmorEquipmentSerializer_v291() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobArmorEquipmentPacket packet) {}
            })
            .updateSerializer(PlayerHotbarPacket.class, new PlayerHotbarSerializer_v291() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, PlayerHotbarPacket packet) {}
            })
            .updateSerializer(PlayerSkinPacket.class, new PlayerSkinSerializer_v390() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, PlayerSkinPacket packet) {}
            })
            .updateSerializer(SetEntityDataPacket.class, new SetEntityDataSerializer_v557() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityDataPacket packet) {}
            })
            .updateSerializer(SetEntityMotionPacket.class, new SetEntityMotionSerializer_v662() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityMotionPacket packet) {}
            })
            .updateSerializer(SetEntityLinkPacket.class, new SetEntityLinkSerializer_v291() {
                @Override public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, SetEntityLinkPacket packet) {}
            })
            // Valid serverbound packets where reading of some fields can be skipped
            .updateSerializer(MobEquipmentPacket.class, new MobEquipmentSerializer_v291() {
                @Override
                public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, MobEquipmentPacket packet) {
                    packet.setRuntimeEntityId(VarInts.readUnsignedLong(buffer));
                    fakeItemRead(buffer);
                    packet.setInventorySlot(buffer.readUnsignedByte());
                    packet.setHotbarSlot(buffer.readUnsignedByte());
                    packet.setContainerId(buffer.readByte());
                }
            })
            // // Illegal bidirectional packets
            .updateSerializer(DebugInfoPacket.class, setIllegalSerializer())
            .updateSerializer(EditorNetworkPacket.class, setIllegalSerializer())
            .updateSerializer(ScriptMessagePacket.class, setIllegalSerializer())
            // // Ignored bidirectional packets
            .updateSerializer(DisconnectPacket.class, setIgnoredSerializer())
            .updateSerializer(SimpleEventPacket.class, setIgnoredSerializer())
            .updateSerializer(TickSyncPacket.class, setIgnoredSerializer())
            .updateSerializer(MultiplayerSettingsPacket.class, setIgnoredSerializer())
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

    private static <T extends BedrockPacket> BedrockPacketSerializer<T> setIllegalSerializer() {
        return new BedrockPacketSerializer<T>() {
            @Override
            public void serialize(ByteBuf buffer, BedrockCodecHelper helper, T packet) {
                throw new IllegalArgumentException("Server tried to send unused packet " + packet.getClass().getSimpleName() + "!");
            }
    
            @Override
            public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, T packet) {
                throw new IllegalArgumentException("Client tried to send unused packet " + packet.getClass().getSimpleName() + "!");
            }
        };
    }

    private static <T extends BedrockPacket> BedrockPacketSerializer<T> setIgnoredSerializer() {
        return new BedrockPacketSerializer<T>() {
            @Override
            public void serialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            }
    
            @Override
            public void deserialize(ByteBuf buffer, BedrockCodecHelper helper, BedrockPacket packet) {
            }
        };
    }
    

    private GameProtocol() {
    }
}
