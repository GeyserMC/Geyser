/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.sponge;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import org.geysermc.connector.common.ping.GeyserPingInfo;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.StatusClient;
import org.spongepowered.api.profile.GameProfile;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Optional;

public class GeyserSpongePingPassthrough implements IGeyserPingPassthrough {

    private static final Cause CAUSE = Cause.of(EventContext.empty(), Sponge.server());

    private static Method SpongeStatusResponse_create;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public GeyserPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        // come on Sponge, this is in commons, why not expose it :(
        ClientPingServerEvent event;
        try {
            if (SpongeStatusResponse_create == null) {
                Class SpongeStatusResponse = Class.forName("org.spongepowered.common.network.status.SpongeStatusResponse");
                Class MinecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
                SpongeStatusResponse_create = SpongeStatusResponse.getDeclaredMethod("create", MinecraftServer);
            }

            Object response = SpongeStatusResponse_create.invoke(null, Sponge.server());
            event = SpongeEventFactory.createClientPingServerEvent(CAUSE, new GeyserStatusClient(inetSocketAddress), (ClientPingServerEvent.Response) response);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Sponge.eventManager().post(event);
        GeyserPingInfo geyserPingInfo = new GeyserPingInfo(
                MessageTranslator.convertMessage(event.response().description()),
                new GeyserPingInfo.Players(
                        event.response().players().orElseThrow(IllegalStateException::new).max(),
                        event.response().players().orElseThrow(IllegalStateException::new).online()
                ),
                new GeyserPingInfo.Version(
                        event.response().version().name(),
                        MinecraftConstants.PROTOCOL_VERSION) // thanks for also not exposing this sponge
        );
        event.response().players().get().profiles().stream()
                .map(GameProfile::name)
                .map(op -> op.orElseThrow(IllegalStateException::new))
                .forEach(geyserPingInfo.getPlayerList()::add);
        return geyserPingInfo;
    }

    private record GeyserStatusClient(InetSocketAddress remote) implements StatusClient {

        @Override
        public InetSocketAddress address() {
            return this.remote;
        }

        @Override
        public MinecraftVersion version() {
            return Sponge.platform().minecraftVersion();
        }

        @Override
        public Optional<InetSocketAddress> virtualHost() {
            return Optional.empty();
        }
    }
}
