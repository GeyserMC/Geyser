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

package org.geysermc.geyser.network;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.network.MessageDirection;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.message.DataType;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageHandler;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.network.message.ByteBufCodec;
import org.geysermc.geyser.network.message.ByteBufMessageBuffer;
import org.geysermc.geyser.session.GeyserSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.geysermc.geyser.util.GeyserMockContext.mockContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MessageDispatchTest {
    private final NetworkChannel xuidChannel = new ExternalNetworkChannel(new IdentifierImpl(Key.key("geyser_test", "xuid")), XuidMessage.class);

    @Test
    void handlersInvokedOncePerIncomingPayload() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            AtomicInteger decodeCount = new AtomicInteger();
            AtomicInteger handlerOneCount = new AtomicInteger();
            AtomicInteger handlerTwoCount = new AtomicInteger();

            new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, buffer -> {
                decodeCount.incrementAndGet();
                return new XuidMessage(buffer);
            }, registration))
                    .clientbound(message -> {
                        handlerOneCount.incrementAndGet();
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, buffer -> {
                decodeCount.incrementAndGet();
                return new XuidMessage(buffer);
            }, registration))
                    .clientbound(message -> {
                        handlerTwoCount.incrementAndGet();
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            byte[] payload = encodePayload("test-xuid");

            Message<MessageBuffer> message = network.createMessage(this.xuidChannel, payload, MessageDirection.CLIENTBOUND);
            assertNotNull(message, "Channel should produce a decoded message");
            network.handleMessages(this.xuidChannel, List.of(message), MessageDirection.CLIENTBOUND);

            assertEquals(1, decodeCount.get(), "Payload should be decoded exactly once per channel, not once per definition");
            assertEquals(1, handlerOneCount.get(), "Handler one should be invoked exactly once per incoming payload");
            assertEquals(1, handlerTwoCount.get(), "Handler two should be invoked exactly once per incoming payload");
        });
    }

    @Test
    void handlerMutationsFlowThroughLaterHandlers() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            NetworkChannel mutableChannel = new ExternalNetworkChannel(new IdentifierImpl(Key.key("geyser_test", "mutable_xuid")), MutableXuidMessage.class);

            new NetworkDefinitionBuilder<MutableXuidMessage>(registration -> network.onRegister(mutableChannel, MutableXuidMessage::new, registration))
                    .clientbound(message -> {
                        assertEquals("xuid-test", message.xuid());

                        message.xuid("xuid-test-1");
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            new NetworkDefinitionBuilder<MutableXuidMessage>(registration -> network.onRegister(mutableChannel, MutableXuidMessage::new, registration))
                    .clientbound(message -> {
                        assertEquals("xuid-test-1", message.xuid());

                        message.xuid("xuid-test-2");
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            ByteBufMessageBuffer encoded = ByteBufCodec.INSTANCE.createBuffer();
            new MutableXuidMessage("xuid-test").encode(encoded);
            byte[] payload = encoded.serialize();

            Message<MessageBuffer> message = network.createMessage(mutableChannel, payload, MessageDirection.CLIENTBOUND);
            assertNotNull(message, "Channel should produce a decoded message");
            network.handleMessages(mutableChannel, List.of(message), MessageDirection.CLIENTBOUND);

            MutableXuidMessage finalMessage = (MutableXuidMessage) message;
            assertEquals("xuid-test-2", finalMessage.xuid(), "Handlers should observe modifications from earlier handlers");
        });
    }

    private static byte[] encodePayload(String xuid) {
        ByteBufMessageBuffer buffer = ByteBufCodec.INSTANCE.createBuffer();
        new XuidMessage(xuid).encode(buffer);
        return buffer.serialize();
    }

    public record XuidMessage(String xuid) implements Message.Simple {

        public XuidMessage(MessageBuffer buffer) {
            this(buffer.read(DataType.STRING));
        }

        @Override
        public void encode(@NonNull MessageBuffer buffer) {
            buffer.write(DataType.STRING, this.xuid);
        }
    }

    public static final class MutableXuidMessage implements Message.Simple {
        private String xuid;

        public MutableXuidMessage(String xuid) {
            this.xuid = xuid;
        }

        public MutableXuidMessage(MessageBuffer buffer) {
            this(buffer.read(DataType.STRING));
        }

        public String xuid() {
            return this.xuid;
        }

        public void xuid(String xuid) {
            this.xuid = xuid;
        }

        @Override
        public void encode(@NonNull MessageBuffer buffer) {
            buffer.write(DataType.STRING, this.xuid);
        }
    }
}
