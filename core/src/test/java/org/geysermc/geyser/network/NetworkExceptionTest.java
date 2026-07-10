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
import org.geysermc.geyser.api.network.NetworkApiException;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.NetworkDispatchException;
import org.geysermc.geyser.api.network.NetworkRegistrationException;
import org.geysermc.geyser.api.network.message.DataType;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageHandler;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.geysermc.geyser.util.GeyserMockContext.mockContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkExceptionTest {
    private final NetworkChannel xuidChannel = new ExternalNetworkChannel(new IdentifierImpl(Key.key("my_extension", "xuid")), XuidMessage.class);

    @Test
    void registeringWithoutAnyHandlerThrows() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            NetworkRegistrationException ex = assertThrows(NetworkRegistrationException.class, () ->
                    new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration))
                            .register()
            );

            assertNull(ex.source(), "no channel was involved yet, so blame should be unattributed");
            assertTrue(ex.getMessage().contains("clientbound") && ex.getMessage().contains("serverbound") && ex.getMessage().contains("bidirectional"), "the message should tell the user what to call instead, but was: " + ex.getMessage());
            assertTrue(ex.getMessage().startsWith("[Geyser Network API]"), "messages should be tagged so log readers can grep them, but was: " + ex.getMessage());
        });
    }

    @Test
    void mixingBidirectionalAndSidedThrows() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            // The public Sided/Bidirectional interfaces prevent chaining both, but a misbehaving
            // implementation could still reach the underlying builder and set both fields. Validate
            // the safety net through the impl directly.
            NetworkDefinitionBuilder<XuidMessage> builder = new NetworkDefinitionBuilder<>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration));
            builder.bidirectional((message, direction) -> MessageHandler.State.UNHANDLED);
            builder.clientbound(message -> MessageHandler.State.UNHANDLED);

            NetworkRegistrationException ex = assertThrows(NetworkRegistrationException.class, builder::register);

            assertTrue(ex.getMessage().contains("bidirectional") && ex.getMessage().contains("clientbound"), "message should name both conflicting kinds, but was: " + ex.getMessage());
        });
    }

    @Test
    void doubleRegisterThrows() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            NetworkDefinitionBuilder<XuidMessage> builder = new NetworkDefinitionBuilder<>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration));
            builder.clientbound(message -> MessageHandler.State.UNHANDLED).register();

            NetworkRegistrationException ex = assertThrows(NetworkRegistrationException.class, builder::register);
            assertTrue(ex.getMessage().contains("already"), "message should make the duplicate call obvious, but was: " + ex.getMessage());
        });
    }

    @Test
    void blankPipelineTagThrows() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            NetworkRegistrationException ex = assertThrows(NetworkRegistrationException.class, () ->
                    new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration))
                            .clientbound(message -> MessageHandler.State.UNHANDLED)
                            .pipeline(pipeline -> pipeline.tag("   "))
                            .register()
            );

            assertTrue(ex.getMessage().contains("blank"), "message should explain the blank tag rule, but was: " + ex.getMessage());
        });
    }

    @Test
    void javaPacketChannelWithoutProtocolStateBlamesExtension() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            // Build a Java packet channel directly so we don't have to spin up the provider registry.
            NetworkChannel javaPacketChannel = new RawPacketChannelImpl(
                    new IdentifierImpl(Key.key("my_extension", "java_raw_packet_2")),
                    true,
                    2,
                    XuidMessage.class
            );

            NetworkRegistrationException ex = assertThrows(NetworkRegistrationException.class, () ->
                    new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(javaPacketChannel, XuidMessage::new, registration))
                            .clientbound(message -> MessageHandler.State.UNHANDLED)
                            .register()
            );

            assertEquals("my_extension", ex.source(), "the channel's namespace should be attributed");
            assertTrue(ex.getMessage().contains("protocolState"), "message should tell the user exactly which builder call to add, but was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("my_extension:java_raw_packet_2"), "message should include the channel identifier so logs are actionable, but was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("responsible party: 'my_extension'"), "the blame suffix should name the responsible party, but was: " + ex.getMessage());
        });
    }

    @Test
    void missingPipelineAnchorThrowsAndNamesTheAnchor() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            NetworkRegistrationException ex = assertThrows(NetworkRegistrationException.class, () ->
                    new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration))
                            .clientbound(message -> MessageHandler.State.UNHANDLED)
                            .pipeline(pipeline -> pipeline.before("does-not-exist"))
                            .register()
            );

            assertEquals("my_extension", ex.source());
            assertTrue(ex.getMessage().contains("does-not-exist"), "message should name the missing anchor tag, but was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("before"), "message should name whether before or after was used, but was: " + ex.getMessage());
        });
    }

    @Test
    void handlerExceptionDuringDispatchWrapsWithBlameAndCause() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            RuntimeException handlerFailure = new RuntimeException("boom from extension code");

            new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(message -> {
                        throw handlerFailure;
                    })
                    .register();

            NetworkDispatchException ex = assertThrows(NetworkDispatchException.class, () ->
                    network.handleMessages(this.xuidChannel, List.of(new XuidMessage("test-xuid")), MessageDirection.CLIENTBOUND));

            assertSame(handlerFailure, ex.getCause(), "original cause must be preserved for stack-trace forensics");
            assertEquals("my_extension", ex.source(), "blame should be the channel's namespace");
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("RuntimeException"), "message should call out the thrown exception type, but was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("my_extension:xuid"), "message should include the channel identifier so admins know what to look at, but was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("clientbound"), "message should name the direction the dispatch was going, but was: " + ex.getMessage());
        });
    }

    @Test
    void networkApiExceptionFromHandlerIsNotRewrapped() {
        mockContext(context -> {
            GeyserSession session = context.mock(GeyserSession.class);
            GeyserNetwork network = new GeyserNetwork(session);

            NetworkDispatchException originallyThrown = new NetworkDispatchException("downstream_extension", "pre-wrapped failure");

            new NetworkDefinitionBuilder<XuidMessage>(registration -> network.onRegister(this.xuidChannel, XuidMessage::new, registration))
                    .clientbound(message -> {
                        throw originallyThrown;
                    })
                    .register();

            NetworkApiException ex = assertThrows(NetworkApiException.class, () ->
                    network.handleMessages(this.xuidChannel, List.of(new XuidMessage("test-xuid")), MessageDirection.CLIENTBOUND));

            assertSame(originallyThrown, ex, "already-attributed Network API exceptions should propagate unchanged");
            assertEquals("downstream_extension", ex.source());
        });
    }

    @Test
    void unattributedExceptionsOmitBlameSuffix() {
        NetworkRegistrationException ex = new NetworkRegistrationException(null, "anonymous failure");
        assertTrue(ex.getMessage().startsWith("[Geyser Network API] anonymous failure"));
        assertFalse(ex.getMessage().contains("responsible party"), "no source means no blame suffix");
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
