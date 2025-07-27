/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.bedrock.SessionDefineNetworkChannelsEvent;
import org.geysermc.geyser.api.event.java.ServerReceiveNetworkMessageEvent;
import org.geysermc.geyser.api.network.MessageDirection;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.NetworkManager;
import org.geysermc.geyser.api.network.PacketChannel;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageCodec;
import org.geysermc.geyser.api.network.message.MessageFactory;
import org.geysermc.geyser.network.message.BedrockPacketMessage;
import org.geysermc.geyser.network.message.ByteBufCodec;
import org.geysermc.geyser.network.message.ByteBufMessageBuffer;
import org.geysermc.geyser.network.message.JavaPacketMessage;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class GeyserNetworkManager implements NetworkManager {
    private final GeyserSession session;

    private final Map<NetworkChannel, MessageDefinition<?>> definitions = new HashMap<>();
    private final Int2ObjectMap<PacketChannel> packetChannels = new Int2ObjectOpenHashMap<>();

    public GeyserNetworkManager(GeyserSession session) {
        this.session = session;

        SessionDefineNetworkChannelsEvent event = new SessionDefineNetworkChannelsEvent(session) {

            @Override
            public void register(@NonNull NetworkChannel channel, @NonNull MessageFactory<MessageBuffer> messageFactory) {
                GeyserNetworkManager.this.registerMessage(channel, new MessageDefinition<>(ByteBufCodec.INSTANCE, messageFactory));
            }

            @Override
            public <T extends MessageBuffer> void register(@NonNull NetworkChannel channel, @NonNull MessageCodec<T> codec, @NonNull MessageFactory<T> messageFactory) {
                GeyserNetworkManager.this.registerMessage(channel, new MessageDefinition<>(codec, messageFactory));
            }
        };

        GeyserApi.api().eventBus().fire(event);
    }

    @Override
    public @NonNull Set<NetworkChannel> getRegisteredChannels() {
        return Set.copyOf(this.definitions.keySet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends MessageBuffer> void send(@NonNull NetworkChannel channel, @NonNull Message<T> message, @NonNull MessageDirection direction) {
        if (channel.isPacket() && message instanceof Message.PacketBase<T> packetBase) {
            if (packetBase instanceof BedrockPacketMessage packetMessage) {
                this.session.sendUpstreamPacket(packetMessage.packet());
            } else if (packetBase instanceof JavaPacketMessage packetMessage) {
                this.session.sendDownstreamPacket(packetMessage.packet());
            } else if (packetBase instanceof Message.Packet packet) {
                PacketChannel packetChannel = (PacketChannel) channel;
                int packetId = packetChannel.packetId();

                ByteBufMessageBuffer buffer = ByteBufCodec.INSTANCE_LE.createBuffer();
                packet.encode(buffer);

                BedrockCodec codec = this.session.getUpstream().getSession().getCodec();
                BedrockCodecHelper helper = this.session.getUpstream().getCodecHelper();

                BedrockPacket bedrockPacket = codec.tryDecode(helper, buffer.buffer(), packetId);
                if (bedrockPacket == null) {
                    throw new IllegalArgumentException("No Bedrock packet definition found for packet ID: " + packetId);
                }

                // Clientbound packets are sent upstream, serverbound packets are sent downstream
                if (direction == MessageDirection.CLIENTBOUND) {
                    this.session.sendUpstreamPacket(bedrockPacket);
                } else {
                    this.session.getUpstream().getSession().getPacketHandler().handlePacket(bedrockPacket);
                }
            }

            return;
        }

        MessageDefinition<T> definition = (MessageDefinition<T>) this.definitions.get(channel);
        if (definition == null) {
            throw new IllegalArgumentException("No message definition registered for channel: " + channel);
        }

        T buffer = definition.codec.createBuffer();
        message.encode(buffer);

        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                Key.key(channel.key(), channel.channel()),
                buffer.serialize()
        );

        this.session.sendDownstreamPacket(packet);
    }

    public <T extends MessageBuffer> Message<T> createMessage(@NonNull NetworkChannel channel, byte @NotNull[] data) {
        return this.createMessage0(channel, definition -> definition.createBuffer(data));
    }

    public <T extends MessageBuffer> Message<T> createMessage(@NonNull NetworkChannel channel, @NonNull T buffer) {
        return this.createMessage0(channel, def -> buffer);
    }

    @SuppressWarnings("unchecked")
    private <T extends MessageBuffer> Message<T> createMessage0(@NonNull NetworkChannel channel, @NonNull Function<MessageDefinition<T>, T> creator) {
        MessageDefinition<T> definition = (MessageDefinition<T>) this.definitions.get(channel);
        if (definition == null) {
            throw new IllegalArgumentException("No message definition registered for channel: " + channel);
        }

        T buffer = creator.apply(definition);
        Message<T> message = definition.createMessage(buffer);
        if (message instanceof BedrockPacketMessage packetMessage) {
            packetMessage.postProcess(this.session, (ByteBufMessageBuffer) buffer);
        }
        return message;
    }

    public PacketChannel getPacketChannel(int packetId) {
        return this.packetChannels.get(packetId);
    }

    @SuppressWarnings("unchecked")
    public boolean handlePacket(BedrockPacket packet, MessageDirection direction) {
        if (this.packetChannels.isEmpty()) {
            return true; // Avoid processing anything if we have nothing to handle
        }

        BedrockCodec codec = this.session.getUpstream().getSession().getCodec();
        BedrockPacketDefinition<BedrockPacket> definition = codec.getPacketDefinition((Class<BedrockPacket>) packet.getClass());
        PacketChannel channel = this.getPacketChannel(definition.getId());
        if (channel == null) {
            return true;
        }

        Message<?> message;
        if (channel.packetType().isInstance(packet)) {
            message = new BedrockPacketMessage(packet);
        } else {
            ByteBuf buffer = Unpooled.buffer();
            definition.getSerializer().serialize(buffer, this.session.getUpstream().getCodecHelper(), packet);
            message = this.createMessage(channel, new ByteBufMessageBuffer(ByteBufCodec.INSTANCE_LE, buffer));
        }

        ServerReceiveNetworkMessageEvent event = new ServerReceiveNetworkMessageEvent(this.session, channel, message, direction);
        this.session.getGeyser().eventBus().fire(event);

        // If the event is canceled, we do not want to process the packet further
        return !event.isCancelled();
    }

    private <T extends MessageBuffer> void registerMessage(@NonNull NetworkChannel channel, @NonNull MessageDefinition<T> codec) {
        if (this.definitions.containsKey(channel)) {
            throw new IllegalArgumentException("Channel is already registered: " + channel);
        }

        this.definitions.put(channel, codec);
        if (channel.isPacket()) {
            PacketChannel packetChannel = (PacketChannel) channel;
            int packetId = packetChannel.packetId();
            this.packetChannels.put(packetId, packetChannel);
        }
    }

    public record MessageDefinition<T extends MessageBuffer>(MessageCodec<? extends T> codec, MessageFactory<T> messageFactory) {

        public T createBuffer(byte @NotNull[] data) {
            return this.codec.createBuffer(data);
        }

        public Message<T> createMessage(@NonNull T buffer) {
            return this.messageFactory.create(buffer);
        }
    }
}
