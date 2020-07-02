/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.platform.sponge;

import org.geysermc.connector.common.ping.GeyserPingInfo;
import org.geysermc.connector.ping.IGeyserPingPassthrough;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.StatusClient;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.Optional;

public class GeyserSpongePingPassthrough implements IGeyserPingPassthrough {

    private static final GeyserStatusClient STATUS_CLIENT = new GeyserStatusClient();
    private static final Cause CAUSE = Cause.of(EventContext.empty(), Sponge.getServer());

    private static Method SpongeStatusResponse_create;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public GeyserPingInfo getPingInformation() {
        // come on Sponge, this is in commons, why not expose it :(
        ClientPingServerEvent event;
        try {
            if(SpongeStatusResponse_create == null) {
                Class SpongeStatusResponse = Class.forName("org.spongepowered.common.network.status.SpongeStatusResponse");
                Class MinecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
                SpongeStatusResponse_create = SpongeStatusResponse.getDeclaredMethod("create", MinecraftServer);
            }

            Object response = SpongeStatusResponse_create.invoke(null, Sponge.getServer());
            event = SpongeEventFactory.createClientPingServerEvent(CAUSE, STATUS_CLIENT, (ClientPingServerEvent.Response) response);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Sponge.getEventManager().post(event);
        GeyserPingInfo geyserPingInfo = new GeyserPingInfo(
                event.getResponse().getDescription().toPlain(),
                event.getResponse().getPlayers().orElseThrow(IllegalStateException::new).getOnline(),
                event.getResponse().getPlayers().orElseThrow(IllegalStateException::new).getMax());
        event.getResponse().getPlayers().get().getProfiles().forEach(player -> {
            geyserPingInfo.addPlayer(player.getName().orElseThrow(IllegalStateException::new));
        });
        return geyserPingInfo;
    }

    @SuppressWarnings("NullableProblems")
    private static class GeyserStatusClient implements StatusClient {

        private static final InetSocketAddress FAKE_REMOTE = new InetSocketAddress(Inet4Address.getLoopbackAddress(), 69);

        @Override
        public InetSocketAddress getAddress() {
            return FAKE_REMOTE;
        }

        @Override
        public MinecraftVersion getVersion() {
            return Sponge.getPlatform().getMinecraftVersion();
        }

        @Override
        public Optional<InetSocketAddress> getVirtualHost() {
            return Optional.empty();
        }
    }
}
