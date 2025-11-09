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

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.event.bedrock.SessionDefineNetworkChannelsEvent;
import org.geysermc.geyser.api.network.message.Message;
import org.geysermc.geyser.api.network.message.MessageBuffer;
import org.geysermc.geyser.api.network.message.MessageHandler;
import org.geysermc.geyser.api.network.message.MessagePriority;

import java.util.Objects;
import java.util.function.Consumer;

public class NetworkDefinitionBuilder<M extends Message<? extends MessageBuffer>> implements SessionDefineNetworkChannelsEvent.Builder.Initial<M> {
    private HandlerEntry<M> handler;
    private SidedHandlerEntry<M> clientbound;
    private SidedHandlerEntry<M> serverbound;

    private String tag;
    private String beforeTag;
    private String afterTag;

    private final Consumer<RegistrationImpl<M>> registrationCallback;
    private boolean registered;

    public NetworkDefinitionBuilder(@NonNull Consumer<RegistrationImpl<M>> registrationCallback) {
        this.registrationCallback = registrationCallback;
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Initial<M> pipeline(@NonNull Consumer<Pipeline> pipeline) {
        Objects.requireNonNull(pipeline, "pipeline");
        PipelineImpl impl = new PipelineImpl();
        pipeline.accept(impl);
        this.tag = impl.tag;
        this.beforeTag = impl.beforeTag;
        this.afterTag = impl.afterTag;
        return this;
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Sided<M> clientbound(MessageHandler.@NonNull Sided<M> handler) {
        return this.clientbound(MessagePriority.NORMAL, handler);
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Sided<M> clientbound(@NonNull MessagePriority priority, MessageHandler.@NonNull Sided<M> handler) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
        this.clientbound = new SidedHandlerEntry<>(priority, handler);
        return this;
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Sided<M> serverbound(MessageHandler.@NonNull Sided<M> handler) {
        return this.serverbound(MessagePriority.NORMAL, handler);
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Sided<M> serverbound(@NonNull MessagePriority priority, MessageHandler.@NonNull Sided<M> handler) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
        this.serverbound = new SidedHandlerEntry<>(priority, handler);
        return this;
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Bidirectional<M> bidirectional(@NonNull MessageHandler<M> handler) {
        return this.bidirectional(MessagePriority.NORMAL, handler);
    }

    @Override
    public SessionDefineNetworkChannelsEvent.Builder.@NonNull Bidirectional<M> bidirectional(@NonNull MessagePriority priority, @NonNull MessageHandler<M> handler) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
        this.handler = new HandlerEntry<>(priority, handler);
        return this;
    }

    @Override
    public SessionDefineNetworkChannelsEvent.@NonNull Registration<M> register() {
        Preconditions.checkState(!this.registered, "This message has already been registered");
        Preconditions.checkState(this.handler == null || (this.clientbound == null && this.serverbound == null), "Cannot register both bidirectional and sided handlers for the same message");
        RegistrationImpl<M> registration = new RegistrationImpl<>(
                this.handler,
                this.clientbound,
                this.serverbound,
                this.tag,
                this.beforeTag,
                this.afterTag
        );

        this.registered = true;
        this.registrationCallback.accept(registration);
        return registration;
    }

    private static final class PipelineImpl implements SessionDefineNetworkChannelsEvent.Builder.Pipeline {
        private String tag;
        private String beforeTag;
        private String afterTag;

        @Override
        public SessionDefineNetworkChannelsEvent.Builder.@NonNull Pipeline tag(@NonNull String tag) {
            this.tag = Objects.requireNonNull(tag, "tag");
            return this;
        }

        @Override
        public SessionDefineNetworkChannelsEvent.Builder.@NonNull Pipeline before(@NonNull String tag) {
            this.beforeTag = Objects.requireNonNull(tag, "tag");
            return this;
        }

        @Override
        public SessionDefineNetworkChannelsEvent.Builder.@NonNull Pipeline after(@NonNull String tag) {
            this.afterTag = Objects.requireNonNull(tag, "tag");
            return this;
        }
    }

    public record RegistrationImpl<M extends Message<? extends MessageBuffer>>(
            HandlerEntry<M> handler,
            SidedHandlerEntry<M> clientbound,
            SidedHandlerEntry<M> serverbound,
            String tag,
            String beforeTag,
            String afterTag
    ) implements SessionDefineNetworkChannelsEvent.Registration<M> {
    }

    public record SidedHandlerEntry<T extends Message<? extends MessageBuffer>>(
            MessagePriority priority,
            MessageHandler.Sided<T> handler
    ) {
    }

    public record HandlerEntry<T extends Message<? extends MessageBuffer>>(
            MessagePriority priority,
            MessageHandler<T> handler
    ) {
    }
}
