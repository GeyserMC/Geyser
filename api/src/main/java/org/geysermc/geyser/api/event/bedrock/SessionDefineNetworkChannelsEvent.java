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

package org.geysermc.geyser.api.event.bedrock;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.network.NetworkChannel;
import org.geysermc.geyser.api.network.ProtocolState;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageCodec;
import org.geysermc.geyser.api.network.message.MessageFactory;
import org.geysermc.geyser.api.network.message.MessageHandler;
import org.geysermc.geyser.api.network.message.MessagePriority;

import java.util.function.Consumer;

/**
 * Called whenever Geyser is registering network channels.
 * <p>
 * It is important to note that this event may be called multiple times during a
 * session's lifecycle, as channels can be registered at different stages of the connection.
 * It is always important to check the {@link State} of the connection through {@link #state()}
 * before registering channels to avoid creating multiple listeners that are only intended
 * to be registered once.
 * <p>
 * It is typically advised to register channels at the {@link State#LOGGED_IN} state unless
 * creating a developer tool, such as a packet logger. This is the latest point at which channels
 * can be registered, and is when clients will be fully established. Registering earlier means
 * certain client information is unavailable or potentially unverified (i.e., Bedrock usernames).
 * <p>
 * Registering at earlier points also increases the intensity of an L7 attack as the client is in a
 * far earlier stage of the connection and there more vulnerable to invalid clients early in the
 * connection stage. However, for debug tools that will never be used in a production environment,
 * registering at earlier stages can be useful to capture more information about the connection and
 * the client's behavior during earlier stages of the connection.
 *
 * @since 2.9.2
 */
public abstract class SessionDefineNetworkChannelsEvent extends ConnectionEvent {
    private final State state;

    public SessionDefineNetworkChannelsEvent(@NonNull GeyserConnection connection, @NonNull State state) {
        super(connection);

        this.state = state;
    }

    /**
     * Gets the state of the connection at the time of channel registration.
     *
     * @return the registration state
     */
    @NonNull
    public State state() {
        return this.state;
    }

    /**
     * Defines the registration of a new network channel with a message factory.
     *
     * @param channel the channel to register
     * @param messageFactory the factory used to create messages from the buffer
     * @param <M> the message type created by the factory
     * @return a registration builder to configure handlers
     */
    public abstract <M extends Message<MessageBuffer>> Builder.@NonNull Initial<M> define(@NonNull NetworkChannel channel, @NonNull MessageFactory<MessageBuffer, M> messageFactory);

    /**
     * Defines the registration of a new network channel with a codec and message factory.
     *
     * @param channel the channel to register
     * @param codec the codec to use to encode/decode the buffer
     * @param messageFactory the factory used to create messages from the buffer
     * @param <T> the buffer type
     * @param <M> the message type created by the factory
     * @return a registration builder to configure handlers
     */
    public abstract <T extends MessageBuffer, M extends Message<T>> Builder.@NonNull Initial<M> define(@NonNull NetworkChannel channel, @NonNull MessageCodec<T> codec, @NonNull MessageFactory<T, M> messageFactory);

    /**
     * Registration builder for attaching handlers to a channel.
     *
     * @param <M> the message type
     */
    public interface Builder<M extends Message<? extends MessageBuffer>> {

        /**
         * Configures the pipeline for this handler.
         *
         * @param pipeline the pipeline consumer
         * @return the builder instance
         */
        @NonNull
        Builder<M> pipeline(@NonNull Consumer<Pipeline> pipeline);

        /**
         * Finalizes the registration.
         *
         * @return the completed registration
         */
        @NonNull
        Registration<M> register();

        interface Initial<M extends Message<? extends MessageBuffer>> extends Sided<M>, Bidirectional<M> {

            /**
             * Sets the protocol state for this message.
             * <p>
             * This is required for any message involving Java Edition
             * packets that need to be restricted to certain states.
             *
             * @param state the protocol state
             * @return the initial builder instance
             */
            @NonNull
            Initial<M> protocolState(@NonNull ProtocolState state);

            /**
             * {@inheritDoc}
             */
            @Override
            @NonNull
            Initial<M> pipeline(@NonNull Consumer<Pipeline> pipeline);
        }

        interface Sided<M extends Message<? extends MessageBuffer>> extends Builder<M> {

            /**
             * Registers a clientbound handler.
             */
            @NonNull
            Sided<M> clientbound(MessageHandler.@NonNull Sided<M> handler);

            /**
             * Registers a clientbound handler with a priority.
             */
            @NonNull
            Sided<M> clientbound(@NonNull MessagePriority priority, MessageHandler.@NonNull Sided<M> handler);

            /**
             * Registers a serverbound handler.
             */
            @NonNull
            Sided<M> serverbound(MessageHandler.@NonNull Sided<M> handler);

            /**
             * Registers a serverbound handler with a priority.
             */
            @NonNull
            Sided<M> serverbound(@NonNull MessagePriority priority, MessageHandler.@NonNull Sided<M> handler);

            /**
             * {@inheritDoc}
             */
            @Override
            @NonNull
            Sided<M> pipeline(@NonNull Consumer<Pipeline> pipeline);
        }

        interface Bidirectional<M extends Message<? extends MessageBuffer>> extends Builder<M> {

            /**
             * Registers a bidirectional handler which receives the message and passes
             * through the direction.
             */
            @NonNull
            Bidirectional<M> bidirectional(@NonNull MessageHandler<M> handler);

            /**
             * Registers a bidirectional handler with a priority.
             */
            @NonNull
            Bidirectional<M> bidirectional(@NonNull MessagePriority priority, @NonNull MessageHandler<M> handler);

            /**
             * {@inheritDoc}
             */
            @Override
            @NonNull
            Bidirectional<M> pipeline(@NonNull Consumer<Pipeline> pipeline);
        }

        /**
         * Pipeline configuration for ordering handlers.
         */
        interface Pipeline {

            /**
             * Tags this handler in the pipeline.
             *
             * @param tag the tag to apply
             * @return the pipeline instance
             */
            @NonNull
            Pipeline tag(@NonNull String tag);

            /**
             * Places this handler before the handler with the given tag.
             * <p>
             * The tag used here must be previously defined in a separate handler using {@link #tag(String)}.
             * However, it should be noted that if the specified tag does not exist at the time of registration,
             * the handler will be added to the end of the pipeline without throwing an error.
             *
             * @param tag the tag to place before
             * @return the pipeline instance
             */
            @NonNull
            Pipeline before(@NonNull String tag);

            /**
             * Places this handler after the handler with the given tag.
             * <p>
             * The tag used here must be previously defined in a separate handler using {@link #tag(String)}.
             * However, it should be noted that if the specified tag does not exist at the time of registration,
             * the handler will be added to the end of the pipeline without throwing an error.
             *
             * @param tag the tag to place after
             * @return the pipeline instance
             */
            @NonNull
            Pipeline after(@NonNull String tag);
        }
    }

    public interface Registration<M extends Message<? extends MessageBuffer>> {
    }

    /**
     * The state of the connection at the time of channel registration.
     * <p>
     * This allows for checking what state the client is in before
     * registering channels, which offers greater flexibility and control
     * for extensions that need to register channels.
     */
    public enum State {
        /**
         * Called when a session is FIRST initialized. The session
         * may lack certain information at this point, such as the Java
         * session information, or verifiable Bedrock client info. However,
         * this is the earliest point at which channels can be registered,
         * and when it is guaranteed a session will have a protocol version.
         */
        INITIALIZED,
        /**
         * Called when a Bedrock session is fully authenticated and has
         * completed the Bedrock login process. At this point, the session
         * will not have yet logged into the Java server, so Java client
         * information will still be unavailable.
         */
        AUTHENTICATED,
        /**
         * Called once a session has logged into the Java server. At this point,
         * the session can be considered fully established, and all information
         * about the client and connection will be available. This is the latest
         * point at which channels can be registered.
         */
        LOGGED_IN
    }
}
