/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.gametest.tests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderGetter;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.configuration.ConfigurationPacketTypes;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.Identifier;
import org.geysermc.geyser.gametest.util.GeyserCodecs;
import org.geysermc.geyser.registry.PacketTranslatorRegistry;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.packet.PacketRegistry;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class JavaPacketTranslatorExistenceTest extends GeyserTestInstance {
    public static final MapCodec<JavaPacketTranslatorExistenceTest> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance)
            .and(PacketInfo.MAP_CODEC.forGetter(test -> test.packet))
            .apply(instance, JavaPacketTranslatorExistenceTest::new)
    );
    // List of packets handled by MCPL in ClientListener
    private static final List<PacketType<?>> MCPL_HANDLED_PACKETS = List.of(
        CommonPacketTypes.CLIENTBOUND_DISCONNECT,
        GamePacketTypes.CLIENTBOUND_START_CONFIGURATION,
        CommonPacketTypes.CLIENTBOUND_TRANSFER,
        ConfigurationPacketTypes.CLIENTBOUND_FINISH_CONFIGURATION,
        ConfigurationPacketTypes.CLIENTBOUND_SELECT_KNOWN_PACKS
    );

    private final PacketInfo packet;

    private JavaPacketTranslatorExistenceTest(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required, PacketInfo packet) {
        super(testEnvironments, required);
        this.packet = packet;
    }

    public static Stream<JavaPacketTranslatorExistenceTest> createForProtocol(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required,
                                                                              ProtocolInfo.DetailsProvider protocol) {
        ProtocolInfo.Details details = protocol.details();
        List<JavaPacketTranslatorExistenceTest> tests = new ObjectArrayList<>();
        details.listPackets((packet, networkId) -> {
            if (!MCPL_HANDLED_PACKETS.contains(packet)) {
                tests.add(new JavaPacketTranslatorExistenceTest(testEnvironments, required, PacketInfo.create(details, packet.id(), networkId)));
            }
        });
        return tests.stream();
    }

    public Identifier packetId() {
        return packet.identifier.withPrefix(packet.protocolFlow.protocol.id() + "/");
    }

    @Override
    public void run(GameTestHelper helper) {
        PacketRegistry mcplPacketRegistry = MinecraftCodec.CODEC.getCodec(packet.protocolFlow.mcplState());

        Class<? extends Packet> mcplPacketClass;
        try {
            mcplPacketClass = packet.protocolFlow.flow == PacketFlow.CLIENTBOUND
                ? mcplPacketRegistry.getClientboundClass(packet.networkId)
                : mcplPacketRegistry.getServerboundClass(packet.networkId);
        } catch (IllegalArgumentException exception) {
            helper.fail("MCPL must have a packet registered for " + packet);
            return;
        }

        boolean hasTranslator = Registries.JAVA_PACKET_TRANSLATORS.get(mcplPacketClass) != null;
        boolean isIgnored = PacketTranslatorRegistry.isIgnoredPacket(mcplPacketClass);

        helper.assertTrue(hasTranslator || isIgnored,
            "Missing translator for " + packet.identifier + "/" + mcplPacketClass.getSimpleName() + " (or must explicitly be marked as ignored)");
        helper.assertFalse(hasTranslator == isIgnored,
            "Packet " + packet.identifier + "/" + mcplPacketClass.getSimpleName() + " must either have a translator or be marked as ignored, not both");
        helper.succeed();
    }

    @Override
    public MapCodec<JavaPacketTranslatorExistenceTest> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Geyser Packet Translator Existence Test: " + packet.identifier);
    }

    // Simplified ProtocolInfo, sort of
    private record ProtocolFlow(ConnectionProtocol protocol, PacketFlow flow) {
        private static final Codec<ConnectionProtocol> CONNECTION_PROTOCOL_CODEC = GeyserCodecs.enumCodec(ConnectionProtocol::values, ConnectionProtocol::id);
        private static final Codec<PacketFlow> PACKET_FLOW_CODEC = GeyserCodecs.enumCodec(PacketFlow::values, PacketFlow::id);
        private static final MapCodec<ProtocolFlow> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                CONNECTION_PROTOCOL_CODEC.fieldOf("protocol").forGetter(ProtocolFlow::protocol),
                PACKET_FLOW_CODEC.fieldOf("flow").forGetter(ProtocolFlow::flow)
            ).apply(instance, ProtocolFlow::new)
        );

        private ProtocolState mcplState() {
            return switch (protocol) {
                case HANDSHAKING -> ProtocolState.HANDSHAKE;
                case PLAY -> ProtocolState.GAME;
                case STATUS -> ProtocolState.STATUS;
                case LOGIN -> ProtocolState.LOGIN;
                case CONFIGURATION -> ProtocolState.CONFIGURATION;
            };
        }

        private DataResult<ProtocolInfo.DetailsProvider> details() {
            return switch (protocol) {
                case CONFIGURATION -> switch (flow) {
                    case CLIENTBOUND -> DataResult.success(ConfigurationProtocols.CLIENTBOUND_TEMPLATE);
                    case SERVERBOUND -> DataResult.success(ConfigurationProtocols.SERVERBOUND_TEMPLATE);
                };
                case PLAY -> switch (flow) {
                    case CLIENTBOUND -> DataResult.success(GameProtocols.CLIENTBOUND_TEMPLATE);
                    case SERVERBOUND -> DataResult.success(GameProtocols.SERVERBOUND_TEMPLATE);
                };
                default -> DataResult.error(() -> "Unimplemented protocol: " + protocol.id());
            };
        }

        private DataResult<PacketInfo> lookupPacket(Identifier id) {
            return details().map(ProtocolInfo.DetailsProvider::details)
                .flatMap(details -> {
                    AtomicInteger found = new AtomicInteger(-1);
                    details.listPackets((type, networkId) -> {
                        if (type.id().equals(id)) {
                            found.set(networkId);
                        }
                    });
                    return found.get() == -1 ? DataResult.error(() -> "Unknown packet for protocol: " + protocol.id() + ", flow: " + flow.id() + ": " + id)
                                             : DataResult.success(new PacketInfo(this, id, found.get()));
                });
        }

        private Codec<PacketInfo> packetCodec() {
            return Identifier.CODEC.comapFlatMap(this::lookupPacket, PacketInfo::identifier);
        }

        @Override
        public String toString() {
            return flow.id() + " " + protocol.id();
        }
    }

    private record PacketInfo(ProtocolFlow protocolFlow, Identifier identifier, int networkId) {
        private static final MapCodec<PacketInfo> MAP_CODEC = ProtocolFlow.MAP_CODEC
            .dispatchMap(PacketInfo::protocolFlow, flow -> flow.packetCodec().fieldOf("packet"));

        private static PacketInfo create(ProtocolInfo.Details details, Identifier identifier, int networkId) {
            return new PacketInfo(new ProtocolFlow(details.id(), details.flow()), identifier, networkId);
        }

        @Override
        public String toString() {
            return identifier + " (ID " + networkId + ", protocol: " + protocolFlow + ")";
        }
    }
}
