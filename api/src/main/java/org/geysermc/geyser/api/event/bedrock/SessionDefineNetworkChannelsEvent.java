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
 * @since 2.9.1
 */
public abstract class SessionDefineNetworkChannelsEvent extends ConnectionEvent {

    public SessionDefineNetworkChannelsEvent(@NonNull GeyserConnection connection) {
        super(connection);
    }

    /**
     * Defines the registration of a new network channel with a message factory.
     *
     * @param channel the channel to register
     * @param messageFactory the factory to create messages from the buffer
     * @param <M> the message type created by the factory
     * @return a registration builder to configure handlers
     */
    public abstract <M extends Message<MessageBuffer>> Builder.@NonNull Initial<M> define(@NonNull NetworkChannel channel, @NonNull MessageFactory<MessageBuffer, M> messageFactory);

    /**
     * Defines the registration of a new network channel with a codec and message factory.
     *
     * @param channel the channel to register
     * @param codec the codec to use to encode/decode the buffer
     * @param messageFactory the factory to create messages from the buffer
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
             * Register a clientbound handler.
             */
            @NonNull
            Sided<M> clientbound(MessageHandler.@NonNull Sided<M> handler);

            /**
             * Register a clientbound handler with a priority.
             */
            @NonNull
            Sided<M> clientbound(@NonNull MessagePriority priority, MessageHandler.@NonNull Sided<M> handler);

            /**
             * Register a serverbound handler.
             */
            @NonNull
            Sided<M> serverbound(MessageHandler.@NonNull Sided<M> handler);

            /**
             * Register a serverbound handler with a priority.
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
             * Register a bidirectional handler receiving the message and direction.
             */
            @NonNull
            Bidirectional<M> bidirectional(@NonNull MessageHandler<M> handler);

            /**
             * Register a bidirectional handler with a priority.
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
}
