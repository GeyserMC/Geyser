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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.network.MessageDirection;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.message.DataType;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageHandler;
import org.geysermc.geyser.api.network.message.MessagePriority;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.geysermc.geyser.util.GeyserMockContext.mockContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class MessageRegistrationOrderTest {
    private final NetworkChannel xuidChannel = new ExternalNetworkChannel(new IdentifierImpl(Key.key("geyser_test", "xuid")), XuidMessage.class);

    @Test
    void testRegistrationOrder() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetworkManager manager = new GeyserNetworkManager(session);

            AtomicInteger state = new AtomicInteger(0);

            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(MessagePriority.EARLY, message -> {
                        assertEquals(0, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(MessagePriority.LATE, message -> {
                        assertEquals(2, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(MessagePriority.NORMAL, message -> {
                        assertEquals(1, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            manager.handleMessages(this.xuidChannel, List.of(new XuidMessage("test-xuid")), MessageDirection.CLIENTBOUND);
        });
    }

    @Test
    void testPipelineTags() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetworkManager manager = new GeyserNetworkManager(session);

            AtomicInteger state = new AtomicInteger(0);

            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(message -> {
                        assertEquals(2, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .pipeline(pipeline -> {
                        pipeline.tag("monitor");
                    })
                    .register();

            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(message -> {
                        assertEquals(1, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .pipeline(pipeline -> {
                        pipeline.tag("initial-handler");
                        pipeline.before("monitor");
                    })
                    .register();

            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(message -> {
                        assertEquals(3, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .pipeline(pipeline -> {
                        pipeline.tag("tail");
                        pipeline.after("monitor");
                    })
                    .register();

            // No pipeline, so should automatically be added to the tail
            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(message -> {
                        assertEquals(4, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            // Early priority - should come first regardless of tail structure
            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(MessagePriority.EARLY, message -> {
                        assertEquals(0, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            manager.handleMessages(this.xuidChannel, List.of(new XuidMessage("test-xuid")), MessageDirection.CLIENTBOUND);
        });
    }

    @Test
    void testMixedHandlers() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetworkManager manager = new GeyserNetworkManager(session);

            AtomicInteger state = new AtomicInteger(0);

            // Simple early clientbound
            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(MessagePriority.EARLY, message -> {
                        assertEquals(0, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            // Late clientbound but first serverbound
            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(MessagePriority.LATE, message -> {
                        assertEquals(2, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .serverbound(MessagePriority.FIRST, message -> {
                        fail("Serverbound handler should not be called in this test");
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            // Normal (default) bidirectional
            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .bidirectional((message, direction) -> {
                        if (direction == MessageDirection.SERVERBOUND) {
                            fail("Serverbound handler should not be called in this test");
                            return MessageHandler.State.UNHANDLED;
                        }

                        assertEquals(1, state.getAndIncrement());
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            // Serverbound only - should never be called
            new NetworkDefinitionBuilder<XuidMessage>(registration -> manager.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .serverbound(MessagePriority.NORMAL, message -> {
                        fail("Serverbound handler should not be called in this test");
                        return MessageHandler.State.UNHANDLED;
                    })
                    .register();

            manager.handleMessages(this.xuidChannel, List.of(new XuidMessage("test-xuid")), MessageDirection.CLIENTBOUND);
        });
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
}
