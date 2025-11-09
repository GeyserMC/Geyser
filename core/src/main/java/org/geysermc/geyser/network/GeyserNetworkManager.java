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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.BedrockPacketDefinition;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.bedrock.SessionDefineNetworkChannelsEvent;
import org.geysermc.geyser.api.network.MessageDirection;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.NetworkManager;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageCodec;
import org.geysermc.geyser.api.network.message.MessageFactory;
import org.geysermc.geyser.api.network.message.MessageHandler;
import org.geysermc.geyser.network.message.BedrockPacketMessage;
import org.geysermc.geyser.network.message.ByteBufCodec;
import org.geysermc.geyser.network.message.ByteBufMessageBuffer;
import org.geysermc.geyser.network.message.JavaPacketMessage;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class GeyserNetworkManager implements NetworkManager {
    private final GeyserSession session;

    private final Map<NetworkChannel, List<MessageDefinition<?, ?>>> definitions = new LinkedHashMap<>();
    private final Int2ObjectMap<PacketChannel> packetChannels = new Int2ObjectOpenHashMap<>();

    public GeyserNetworkManager(GeyserSession session) {
        this.session = session;

        SessionDefineNetworkChannelsEvent event = new SessionDefineNetworkChannelsEvent(session) {

            @Override
            public <M extends Message<MessageBuffer>> Builder.@NonNull Initial<M> define(@NonNull NetworkChannel channel, @NonNull MessageFactory<MessageBuffer, M> messageFactory) {
                return new NetworkDefinitionBuilder<>(registration -> onRegister(channel, ByteBufCodec.INSTANCE, messageFactory, registration));
            }

            @Override
            public <T extends MessageBuffer, M extends Message<T>> Builder.@NonNull Initial<M> define(@NonNull NetworkChannel channel, @NonNull MessageCodec<T> codec, @NonNull MessageFactory<T, M> messageFactory) {
                return new NetworkDefinitionBuilder<>(registration -> onRegister(channel, codec, messageFactory, registration));
            }
        };

        GeyserImpl.getInstance().getEventBus().fire(event);
    }

    @VisibleForTesting
    <M extends Message<MessageBuffer>> void onRegister(@NonNull NetworkChannel channel, @NonNull MessageFactory<MessageBuffer, M> messageFactory, NetworkDefinitionBuilder.@NonNull RegistrationImpl<M> registration) {
        this.onRegister(channel, ByteBufCodec.INSTANCE, messageFactory, registration);
    }

    @SuppressWarnings("unchecked")
    private <T extends MessageBuffer, M extends Message<T>> void onRegister(@NonNull NetworkChannel channel, @NonNull MessageCodec<? extends T> codec,
                                                                           @NonNull MessageFactory<T, M> messageFactory, NetworkDefinitionBuilder.@NonNull RegistrationImpl<M> registration) {
        MessageHandler<M> handler;
        int priority;

        NetworkDefinitionBuilder.HandlerEntry<M> bidirectional = registration.handler();
        NetworkDefinitionBuilder.SidedHandlerEntry<M> clientbound = registration.clientbound();
        NetworkDefinitionBuilder.SidedHandlerEntry<M> serverbound = registration.serverbound();

        if (bidirectional != null) {
            handler = bidirectional.handler();
            priority = bidirectional.priority() != null ? bidirectional.priority().value() : 0;
        } else {
            handler = null;

            int cbPriority = clientbound != null && clientbound.priority() != null ? clientbound.priority().value() : Integer.MIN_VALUE;
            int sbPriority = serverbound != null && serverbound.priority() != null ? serverbound.priority().value() : Integer.MIN_VALUE;
            priority = Math.max(cbPriority, sbPriority);

            if (priority == Integer.MIN_VALUE) {
                priority = 0;
            }
        }

        MessageDefinition<T, M> definition = new MessageDefinition<>((MessageCodec<T>) codec,
                messageFactory,
                handler,
                clientbound != null ? clientbound.handler() : null,
                serverbound != null ? serverbound.handler() : null,
                priority,
                clientbound != null && clientbound.priority() != null ? clientbound.priority().value() : null,
                serverbound != null && serverbound.priority() != null ? serverbound.priority().value() : null,
                registration.tag(),
                registration.beforeTag(),
                registration.afterTag()
        );

        this.registerMessage(channel, definition);
    }

    @Override
    @Nonnull
    public Set<NetworkChannel> registeredChannels() {
        return Set.copyOf(this.definitions.keySet());
    }

    @Override
    public <T extends MessageBuffer> void send(@NonNull NetworkChannel channel, @NonNull Message<T> message, @NonNull MessageDirection direction) {
        if (channel.isPacket() && message instanceof Message.PacketBase<T> packetBase) {
            if (packetBase instanceof BedrockPacketMessage<?> packetMessage) {
                this.session.sendUpstreamPacket(packetMessage.packet());
            } else if (packetBase instanceof JavaPacketMessage<?> packetMessage) {
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

        MessageDefinition<T, Message<T>> definition = this.findMessageDefinition(channel, message);

        T buffer = definition.codec.createBuffer();
        message.encode(buffer);

        ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
                Key.key(channel.identifier().toString()),
                buffer.serialize()
        );

        this.session.sendDownstreamPacket(packet);
    }

    @NonNull
    public <T extends MessageBuffer> List<Message<T>> createMessages(@NonNull NetworkChannel channel, byte @NonNull[] data) {
        return this.createMessages0(channel, definition -> definition.createBuffer(data));
    }

    @NonNull
    public <T extends MessageBuffer> List<Message<T>> createMessages(@NonNull NetworkChannel channel, @NonNull T buffer) {
        return this.createMessages0(channel, def -> buffer);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private <T extends MessageBuffer, M extends Message<T>> List<M> createMessages0(@NonNull NetworkChannel channel, @NonNull Function<MessageDefinition<T, M>, T> creator) {
        List<MessageDefinition<?, ?>> definitions = this.definitions.get(channel);
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("No message definition registered for channel: " + channel);
        }

        List<M> messages = new ArrayList<>();
        for (MessageDefinition<?, ?> def : definitions) {
            MessageDefinition<T, M> definition = (MessageDefinition<T, M>) def;
            T buffer = creator.apply(definition);
            M message = definition.createMessage(buffer);
            if (message instanceof BedrockPacketMessage<?> packetMessage) {
                packetMessage.postProcess(this.session, (ByteBufMessageBuffer) buffer);
            }

            messages.add(message);
        }

        return messages;
    }

    @Nullable
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

        List<Message<ByteBufMessageBuffer>> messages;
        if (channel.messageType().isInstance(packet)) {
            messages = List.of(new BedrockPacketMessage<>(packet));
        } else {
            ByteBuf buffer = Unpooled.buffer();
            definition.getSerializer().serialize(buffer, this.session.getUpstream().getCodecHelper(), packet);
            messages = this.createMessages(channel, new ByteBufMessageBuffer(ByteBufCodec.INSTANCE_LE, buffer));
        }

        return this.handleMessages(channel, messages, direction);
    }

    @SuppressWarnings("unchecked")
    public <T extends MessageBuffer> boolean handleMessages(@NonNull NetworkChannel channel, @NonNull List<Message<T>> messages, @NonNull MessageDirection direction) {
        List<MessageDefinition<?, ?>> rawList = this.definitions.get(channel);
        if (rawList == null || rawList.isEmpty()) {
            return true;
        }

        // Build a direction-aware ordered list while preserving pipeline tag anchors
        List<MessageDefinition<?, ?>> ordered = new ArrayList<>();
        List<MessageDefinition<?, ?>> unpinnedBlock = new ArrayList<>();
        for (MessageDefinition<?, ?> def : rawList) {
            boolean pinned = def.tag() != null || def.beforeTag() != null || def.afterTag() != null;
            if (pinned) {
                // flush any accumulated unpinned block sorted by effective priority for this direction
                if (!unpinnedBlock.isEmpty()) {
                    unpinnedBlock.sort((a, b) -> Integer.compare(
                            b.priority(direction),
                            a.priority(direction)
                    ));
                    ordered.addAll(unpinnedBlock);
                    unpinnedBlock.clear();
                }
                ordered.add(def);
            } else {
                unpinnedBlock.add(def);
            }
        }
        if (!unpinnedBlock.isEmpty()) {
            unpinnedBlock.sort((a, b) -> Integer.compare(
                    b.priority(direction),
                    a.priority(direction)
            ));
            ordered.addAll(unpinnedBlock);
            unpinnedBlock.clear();
        }

        for (Message<T> message : messages) {
            for (MessageDefinition<?, ?> def : ordered) {
                if (!(channel instanceof BaseNetworkChannel base) || !base.messageType().isInstance(message)) {
                    continue;
                }

                MessageDefinition<T, Message<T>> definition = (MessageDefinition<T, Message<T>>) def;

                MessageHandler.State state;
                if (definition.handler != null) {
                    state = definition.handler.handle(message, direction);
                } else if (direction == MessageDirection.CLIENTBOUND && definition.clientboundHandler != null) {
                    state = definition.clientboundHandler.handle(message);
                } else if (direction == MessageDirection.SERVERBOUND && definition.serverboundHandler != null) {
                    state = definition.serverboundHandler.handle(message);
                } else {
                    continue; // no suitable handler; try next definition
                }

                if (state == MessageHandler.State.HANDLED) {
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private <T extends MessageBuffer, M extends Message<T>> MessageDefinition<T, M> findMessageDefinition(@NonNull NetworkChannel channel, @NonNull Message<T> message) {
        List<MessageDefinition<?, ?>> definitions = this.definitions.get(channel);
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("No message definition registered for channel: " + channel);
        }

        MessageDefinition<T, Message<T>> definition = null;
        for (MessageDefinition<?, ?> def : definitions) {
            if (channel instanceof BaseNetworkChannel baseChannel) {
                if (baseChannel.messageType().isInstance(message)) {
                    definition = (MessageDefinition<T, Message<T>>) def;
                    break;
                }
            }
        }

        if (definition == null) {
            throw new IllegalArgumentException("No suitable message definition found for channel: " + channel + " and message type: " + message.getClass());
        }

        return (MessageDefinition<T, M>) definition;
    }

    private <T extends MessageBuffer, M extends Message<T>> void registerMessage(@NonNull NetworkChannel channel, @NonNull MessageDefinition<T, M> definition) {
        List<MessageDefinition<?, ?>> list = this.definitions.computeIfAbsent(channel, key -> new ArrayList<>());

        // Determine the insert position based on pipeline tags or priority
        int insertIndex = -1;
        if (definition.beforeTag() != null) {
            for (int i = 0; i < list.size(); i++) {
                MessageDefinition<?, ?> existing = list.get(i);
                if (definition.beforeTag().equals(existing.tag())) {
                    insertIndex = i;
                    break;
                }
            }
        } else if (definition.afterTag() != null) {
            for (int i = 0; i < list.size(); i++) {
                MessageDefinition<?, ?> existing = list.get(i);
                if (definition.afterTag().equals(existing.tag())) {
                    insertIndex = i + 1;
                    break;
                }
            }
        }

        if (insertIndex == -1) {
            // Fallback: insert by descending priority
            insertIndex = list.size();
            for (int i = 0; i < list.size(); i++) {
                MessageDefinition<?, ?> existing = list.get(i);
                if (definition.priority() > existing.priority()) {
                    insertIndex = i;
                    break;
                }
            }
        }

        list.add(insertIndex, definition);

        if (channel.isPacket() && channel instanceof PacketChannel packetChannel) {
            int packetId = packetChannel.packetId();
            this.packetChannels.put(packetId, packetChannel);
        }
    }

    public record MessageDefinition<T extends MessageBuffer, M extends Message<T>>(
            @NonNull MessageCodec<T> codec,
            @NonNull MessageFactory<T, M> messageFactory,
            @Nullable MessageHandler<M> handler,
            MessageHandler.Sided<M> clientboundHandler,
            MessageHandler.Sided<M> serverboundHandler,
            int priority,
            @Nullable Integer clientboundPriority,
            @Nullable Integer serverboundPriority,
            @Nullable String tag,
            @Nullable String beforeTag,
            @Nullable String afterTag
    ) {

        @NonNull
        public T createBuffer(byte @NonNull[] data) {
            return this.codec.createBuffer(data);
        }

        @NonNull
        public M createMessage(@NonNull T buffer) {
            return this.messageFactory.create(buffer);
        }

        public int priority(@NonNull MessageDirection direction) {
            if (this.handler != null) {
                return this.priority;
            }

            if (direction == MessageDirection.CLIENTBOUND) {
                return this.clientboundPriority != null ? this.clientboundPriority : 0;
            }

            return this.serverboundPriority != null ? this.serverboundPriority : 0;
        }
    }
}
